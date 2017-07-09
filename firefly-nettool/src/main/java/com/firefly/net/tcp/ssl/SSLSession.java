package com.firefly.net.tcp.ssl;

import com.firefly.net.BufferPool;
import com.firefly.net.SSLContextFactory;
import com.firefly.net.SSLEventHandler;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.net.buffer.ThreadSafeIOBufferPool;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.io.BufferReaderHandler;
import com.firefly.utils.io.BufferUtils;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class SSLSession implements Closeable {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    private static final BufferPool bufferPool = new ThreadSafeIOBufferPool();

    private final Session session;
    private final SSLEngine sslEngine;

    private ByteBuffer inNetBuffer;
    private ByteBuffer outAppBuffer;

    private static final int requestBufferSize = 1024 * 8;
    private static final int writeBufferSize = 1024 * 8;

    /*
     * An empty ByteBuffer for use when one isn't available, say as a source
     * buffer during initial handshake wraps or for close operations.
     */
    private static final ByteBuffer hsBuffer = ByteBuffer.allocateDirect(0);

    /*
     * We have received the shutdown request by our caller, and have closed our
     * outbound side.
     */
    private boolean closed = false;

    /*
     * During our initial handshake, keep track of the next SSLEngine operation
     * that needs to occur:
     *
     * NEED_WRAP/NEED_UNWRAP
     *
     * Once the initial handshake has completed, we can short circuit handshake
     * checks with initialHSComplete.
     */
    private HandshakeStatus initialHSStatus;

    private boolean initialHSComplete;

    private final SSLEventHandler sslEventHandler;

    private final SslHandler sslHandler;

    public SSLSession(SSLContextFactory factory, boolean clientMode, Session session, SSLEventHandler sslEventHandler) throws IOException {
        this(factory.createSSLEngine(clientMode), session, sslEventHandler);
    }

    private SSLSession(SSLEngine sslEngine, Session session, SSLEventHandler sslEventHandler) throws IOException {
        this.session = session;
        this.sslEventHandler = sslEventHandler;
        this.sslEngine = sslEngine;

        outAppBuffer = ByteBuffer.allocate(requestBufferSize);
        initialHSComplete = false;
        sslHandler = new SslHandler(sslEngine);

        // start tls
        this.sslEngine.beginHandshake();
        initialHSStatus = sslEngine.getHandshakeStatus();
        if (sslEngine.getUseClientMode()) {
            doHandshakeResponse();
        }
    }

    /**
     * The initial handshake is a procedure by which the two peers exchange
     * communication parameters until an SSLSession is established. Application
     * data can not be sent during this phase.
     *
     * @param receiveBuffer Encrypted message
     * @return True means handshake success
     * @throws IOException A runtime exception
     */
    private boolean doHandshake(ByteBuffer receiveBuffer) throws IOException {
        if (!session.isOpen()) {
            sslEngine.closeInbound();
            return (initialHSComplete = false);
        }

        if (initialHSComplete) {
            return true;
        }

        switch (initialHSStatus) {
            case NOT_HANDSHAKING:
            case FINISHED: {
                handshakeFinish();
                return initialHSComplete;
            }

            case NEED_UNWRAP:
                doHandshakeReceive(receiveBuffer);
                if (initialHSStatus != HandshakeStatus.NEED_WRAP)
                    break;

            case NEED_WRAP:
                doHandshakeResponse();
                break;

            default: // NEED_TASK
                throw new RuntimeException("Invalid Handshaking State" + initialHSStatus);
        }
        return initialHSComplete;
    }

    private void doHandshakeReceive(ByteBuffer receiveBuffer) throws IOException {
        SSLEngineResult result;

        merge(receiveBuffer);

        needIO:
        while (initialHSStatus == HandshakeStatus.NEED_UNWRAP) {

            unwrap:
            while (true) {
                int netSize = sslEngine.getSession().getPacketBufferSize();
                List<ByteBuffer> inNetBuffers = BufferUtils.split(inNetBuffer, netSize);
                for (ByteBuffer net : inNetBuffers) {
                    //FIXME using direct buffer avoid netty ByteBufAllocator bug
                    ByteBuffer directTmpBuffer = bufferPool.acquire(net.remaining());
                    try {
                        directTmpBuffer.put(net.slice()).flip();
                        result = sslEngine.unwrap(directTmpBuffer, outAppBuffer);
                    } finally {
                        bufferPool.release(directTmpBuffer);
                    }

                    int consumed = result.bytesConsumed();
                    inNetBuffer.position(inNetBuffer.position() + consumed);
                    net.position(net.position() + consumed);

                    initialHSStatus = result.getHandshakeStatus();
                    if (log.isDebugEnabled()) {
                        log.debug("session {} handshake receives data, init: {} | ret: {} | complete: {} ",
                                session.getSessionId(), initialHSStatus, result.getStatus(), initialHSComplete);
                    }
                    switch (result.getStatus()) {
                        case OK:
                            switch (initialHSStatus) {
                                case NEED_TASK:
                                    initialHSStatus = doTasks();
                                    break;

                                case NOT_HANDSHAKING:
                                case FINISHED:
                                    handshakeFinish();
                                    break needIO;
                                default:
                                    break;
                            }
                            break unwrap;

                        case BUFFER_UNDERFLOW:
                            switch (initialHSStatus) {
                                case NOT_HANDSHAKING:
                                case FINISHED:
                                    handshakeFinish();
                                    break needIO;
                            }
                            break needIO;

                        case BUFFER_OVERFLOW:
                            // Reset the application buffer size.
                            int appSize = sslEngine.getSession().getApplicationBufferSize();
                            ByteBuffer b = ByteBuffer.allocate(appSize + outAppBuffer.position());
                            outAppBuffer.flip();
                            b.put(outAppBuffer);
                            outAppBuffer = b;
                            // retry the operation.
                            break;

                        default: // CLOSED:
                            throw new IOException("Received" + result.getStatus() + "during initial handshaking");
                    }

                }


            } // "unwrap" block.
        } // "needIO" block.
    }

    private void handshakeFinish() {
        log.info("session {} handshake success!", session.getSessionId());
        initialHSComplete = true;
        sslEventHandler.handshakeFinished(this);
    }

    private void doHandshakeResponse() throws IOException {
        while (initialHSStatus == HandshakeStatus.NEED_WRAP) {
            SSLEngineResult result;
            ByteBuffer writeBuf = ByteBuffer.allocateDirect(sslEngine.getSession().getPacketBufferSize());

            wrap:
            while (true) {
                result = sslEngine.wrap(hsBuffer, writeBuf);
                initialHSStatus = result.getHandshakeStatus();
                if (log.isDebugEnabled()) {
                    log.debug("session {} handshake response, init: {} | ret: {} | complete: {} ",
                            session.getSessionId(), initialHSStatus, result.getStatus(), initialHSComplete);
                }
                switch (result.getStatus()) {
                    case OK:
                        if (initialHSStatus == HandshakeStatus.NEED_TASK) {
                            initialHSStatus = doTasks();
                        }

                        writeBuf.flip();
                        session.write(writeBuf, Callback.NOOP);
                        break wrap;

                    case BUFFER_OVERFLOW:
                        int netSize = sslEngine.getSession().getPacketBufferSize();
                        ByteBuffer b = ByteBuffer.allocate(writeBuf.position() + netSize);
                        writeBuf.flip();
                        b.put(writeBuf);
                        writeBuf = b;
                        break;

                    default: // BUFFER_UNDERFLOW, CLOSED:
                        throw new IOException("Received " + result.getStatus() + " during initial handshaking");
                }
            }
        }
    }

    private void merge(ByteBuffer now) {
        if (!now.hasRemaining())
            return;

        if (inNetBuffer != null) {
            if (inNetBuffer.hasRemaining()) {
                ByteBuffer ret = ByteBuffer.allocate(inNetBuffer.remaining() + now.remaining());
                ret.put(inNetBuffer).put(now).flip();
                inNetBuffer = ret;
            } else {
                inNetBuffer = now;
            }
        } else {
            inNetBuffer = now;
        }
    }

    private ByteBuffer getOutAppBuffer() {
        outAppBuffer.flip();
        if (outAppBuffer.hasRemaining()) {
            ByteBuffer buf = ByteBuffer.allocate(outAppBuffer.remaining());
            buf.put(outAppBuffer).flip();
            outAppBuffer = ByteBuffer.allocate(requestBufferSize);
            if (log.isDebugEnabled()) {
                log.debug("SSL session {} unwrap, app buffer -> {}", session.getSessionId(), buf.remaining());
            }
            return buf;
        } else {
            return null;
        }
    }

    /**
     * Do all the outstanding handshake tasks in the current Thread.
     *
     * @return The result of handshake
     */
    private SSLEngineResult.HandshakeStatus doTasks() {
        Runnable runnable;

        // We could run this in a separate thread, but do in the current for
        // now.
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            runnable.run();
        }
        return sslEngine.getHandshakeStatus();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            // log.debug("close SSL engine, {}|{}", sslEngine.isInboundDone(),
            // sslEngine.isOutboundDone());
            sslEngine.closeOutbound();
            closed = true;
        }
    }

    public String applicationProtocol() {
        String protocol = sslHandler.applicationProtocol();
        log.debug("selected protocol -> {}", protocol);
        return protocol;
    }

    public boolean isOpen() {
        return !closed;
    }

    /**
     * This method is used to decrypt data, it implied do handshake
     *
     * @param receiveBuffer Encrypted message
     * @return plaintext
     * @throws IOException sslEngine error during data read
     */
    public ByteBuffer read(ByteBuffer receiveBuffer) throws IOException {
        if (!doHandshake(receiveBuffer))
            return null;

        if (!initialHSComplete)
            throw new IllegalStateException("The initial handshake is not complete.");

        if (log.isDebugEnabled()) {
            log.debug("SSL read current session {} status -> {}", session.getSessionId(), session.isOpen());
        }
        merge(receiveBuffer);
        if (!inNetBuffer.hasRemaining()) {
            return null;
        }

        //split net buffer when the net buffer remaining great than the net size
        int netSize = sslEngine.getSession().getPacketBufferSize();
        List<ByteBuffer> inNetBuffers = BufferUtils.split(inNetBuffer, netSize);
        for (ByteBuffer net : inNetBuffers) {
            SSLEngineResult result;
            while (true) {
                if (log.isDebugEnabled()) {
                    log.debug("SSL session {} unwrap, pocket -> {},  in -> {}, out -> {}, temp -> {}",
                            session.getSessionId(), netSize, inNetBuffer.remaining(), outAppBuffer.remaining(),
                            net.remaining());
                }

                //FIXME using direct buffer avoid netty ByteBufAllocator bug
                ByteBuffer directTmpBuffer = bufferPool.acquire(net.remaining());
                try {
                    directTmpBuffer.put(net.slice()).flip();
                    result = sslEngine.unwrap(directTmpBuffer, outAppBuffer);
                } finally {
                    bufferPool.release(directTmpBuffer);
                }

                int consumed = result.bytesConsumed();
                inNetBuffer.position(inNetBuffer.position() + consumed);
                net.position(net.position() + consumed);
                if (log.isDebugEnabled()) {
                    log.debug("SSL session {} unwrap, status -> {}, in -> {}, out -> {}, temp -> {}, consumed -> {}",
                            session.getSessionId(), result.getStatus(), inNetBuffer.remaining(), outAppBuffer.remaining(),
                            net.remaining(), consumed);
                }

                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        // Reset the application buffer size.
                        int appSize = sslEngine.getSession().getApplicationBufferSize();
                        ByteBuffer b = ByteBuffer.allocate(appSize + outAppBuffer.position());
                        outAppBuffer.flip();
                        b.put(outAppBuffer);
                        outAppBuffer = b;
                        // retry the operation.
                        break;

                    case BUFFER_UNDERFLOW:
                        return getOutAppBuffer();

                    case OK:
                        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                            doTasks();
                        }
                        if (!inNetBuffer.hasRemaining()) {
                            return getOutAppBuffer();
                        }
                        break;

                    default:
                        throw new IOException("sslEngine error during data read: " + result.getStatus());
                }
            }
        }

        return getOutAppBuffer();
    }

    public int write(ByteBuffer[] outputBuffers, Callback callback) throws IOException {
        int ret = 0;
        CountingCallback countingCallback = new CountingCallback(callback, outputBuffers.length);
        for (ByteBuffer outputBuffer : outputBuffers) {
            ret += write(outputBuffer, countingCallback);
        }
        return ret;
    }

    /**
     * This method is used to encrypt and flush to socket channel
     *
     * @param outputBuffer Plaintext message
     * @return writen length
     * @throws IOException sslEngine error during data write
     */
    public int write(ByteBuffer outputBuffer, Callback callback) throws IOException {
        if (!initialHSComplete)
            throw new IllegalStateException("The initial handshake is not complete.");

        int ret = 0;
        if (!outputBuffer.hasRemaining())
            return ret;

        final int remain = outputBuffer.remaining();

        while (ret < remain) {
            ByteBuffer writeBuf = ByteBuffer.allocateDirect(writeBufferSize);

            wrap:
            while (true) {
                SSLEngineResult result;

                //FIXME using direct buffer avoid netty ByteBufAllocator bug
                ByteBuffer directTmpBuffer = bufferPool.acquire(outputBuffer.remaining());
                try {
                    directTmpBuffer.put(outputBuffer.slice()).flip();
                    result = sslEngine.wrap(directTmpBuffer, writeBuf);
                } finally {
                    bufferPool.release(directTmpBuffer);
                }

                ret += result.bytesConsumed();

                switch (result.getStatus()) {
                    case OK:
                        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                            doTasks();
                        }

                        writeBuf.flip();
                        session.write(writeBuf, callback);
                        break wrap;

                    case BUFFER_OVERFLOW:
                        int netSize = sslEngine.getSession().getPacketBufferSize();
                        ByteBuffer b = ByteBuffer.allocateDirect(writeBuf.position() + netSize);
                        writeBuf.flip();
                        b.put(writeBuf);
                        writeBuf = b;
                        // retry the operation.
                        break;
                    default:
                        throw new IOException("sslEngine error during data write: " + result.getStatus());
                }
            }
        }

        return ret;
    }

    private class FileBufferReaderHandler implements BufferReaderHandler {

        private final long len;

        private FileBufferReaderHandler(long len) {
            this.len = len;
        }

        @Override
        public void readBuffer(ByteBuffer buf, CountingCallback countingCallback, long count) {
            log.debug("write file,  count: {} , lenth: {}", count, len);
            try {
                write(buf, countingCallback);
            } catch (Throwable e) {
                log.error("ssl session writing error", e);
            }
        }

    }

    public long transferFileRegion(FileRegion file, Callback callback) throws Throwable {
        long ret = 0;
        try (FileRegion fileRegion = file) {
            fileRegion.transferTo(callback, new FileBufferReaderHandler(file.getLength()));
        }
        return ret;
    }

    public boolean isHandshakeFinished() {
        return initialHSComplete;
    }
}
