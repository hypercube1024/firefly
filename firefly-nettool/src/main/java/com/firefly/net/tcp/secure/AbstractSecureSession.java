package com.firefly.net.tcp.secure;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.net.SecureSession;
import com.firefly.net.SecureSessionHandshakeListener;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.io.BufferReaderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractSecureSession implements SecureSession {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected static final int requestBufferSize = Integer.getInteger("com.firefly.net.tcp.secure.SecureSession.requestBufferSize", 1024 * 8);
    protected static final int writeBufferSize = Integer.getInteger("com.firefly.net.tcp.secure.SecureSession.writeBufferSize", 1024 * 8);
    protected static final ByteBuffer hsBuffer = ByteBuffer.allocateDirect(0);

    protected final Session session;
    protected final SSLEngine sslEngine;
    protected final ApplicationProtocolSelector applicationProtocolSelector;
    protected final SecureSessionHandshakeListener handshakeListener;

    protected ByteBuffer inNetBuffer;
    protected ByteBuffer outAppBuffer;
    protected boolean closed = false;
    protected SSLEngineResult.HandshakeStatus initialHSStatus;
    protected boolean initialHSComplete;

    public AbstractSecureSession(Session session,
                                 SSLEngine sslEngine,
                                 ApplicationProtocolSelector applicationProtocolSelector,
                                 SecureSessionHandshakeListener handshakeListener) throws IOException {
        this.session = session;
        this.sslEngine = sslEngine;
        this.applicationProtocolSelector = applicationProtocolSelector;
        this.handshakeListener = handshakeListener;

        outAppBuffer = ByteBuffer.allocate(requestBufferSize);
        initialHSComplete = false;

        // start tls
        this.sslEngine.beginHandshake();
        initialHSStatus = sslEngine.getHandshakeStatus();
        if (sslEngine.getUseClientMode()) {
            doHandshakeResponse();
        }
    }

    /**
     * The initial handshake is a procedure by which the two peers exchange
     * communication parameters until an SecureSession is established. Application
     * data can not be sent during this phase.
     *
     * @param receiveBuffer Encrypted message
     * @return True means handshake success
     * @throws IOException A runtime exception
     */
    protected boolean doHandshake(ByteBuffer receiveBuffer) throws IOException {
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
                if (initialHSStatus != SSLEngineResult.HandshakeStatus.NEED_WRAP)
                    break;

            case NEED_WRAP:
                doHandshakeResponse();
                break;

            default: // NEED_TASK
                throw new RuntimeException("Invalid Handshaking State" + initialHSStatus);
        }
        return initialHSComplete;
    }

    protected void doHandshakeReceive(ByteBuffer receiveBuffer) throws IOException {
        merge(receiveBuffer);
        int netSize = sslEngine.getSession().getPacketBufferSize();

        needIO:
        while (initialHSStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {

            unwrap:
            while (true) {
                ByteBuffer buf = splitBuffer(netSize);
                SSLEngineResult result = unwrap(buf);
                initialHSStatus = result.getHandshakeStatus();

                if (log.isDebugEnabled()) {
                    log.debug("Session {} handshake result -> {}, initialHSStatus -> {}, inNetRemain -> {}",
                            session.getSessionId(), result.toString(), initialHSStatus, inNetBuffer.remaining());
                }

                switch (result.getStatus()) {
                    case OK: {
                        switch (initialHSStatus) {
                            case NEED_TASK:
                                initialHSStatus = doTasks();
                                break unwrap;

                            case NOT_HANDSHAKING:
                            case FINISHED:
                                handshakeFinish();
                                break needIO;
                            default:
                                break unwrap;
                        }
                    }

                    case BUFFER_UNDERFLOW: {
                        switch (initialHSStatus) {
                            case NOT_HANDSHAKING:
                            case FINISHED:
                                handshakeFinish();
                                break needIO;
                        }

                        if (inNetBuffer.remaining() >= netSize) {
                            break; // retry the operation.
                        } else {
                            break needIO;
                        }
                    }

                    case BUFFER_OVERFLOW: {
                        // Reset the application buffer size.
                        int appSize = sslEngine.getSession().getApplicationBufferSize();
                        ByteBuffer b = ByteBuffer.allocate(appSize + outAppBuffer.position());
                        outAppBuffer.flip();
                        b.put(outAppBuffer);
                        outAppBuffer = b;
                    }
                    break; // retry the operation.

                    case CLOSED: {
                        log.info("Session {} handshake failure. SSLEngine will close inbound", session.getSessionId());
                        sslEngine.closeInbound();
                    }
                    break needIO;

                    default:
                        throw new IOException(StringUtils.replace("Session {} handshake exception. status -> {}", session.getSessionId(), result.getStatus()));

                }
            }
        }
    }

    protected void handshakeFinish() {
        log.info("session {} handshake success!", session.getSessionId());
        initialHSComplete = true;
        handshakeListener.complete(this);
    }


    protected void doHandshakeResponse() throws IOException {

        outer:
        while (initialHSStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            SSLEngineResult result;
            ByteBuffer writeBuf = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());

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
                        if (initialHSStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
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

                    case CLOSED:
                        log.info("Session {} handshake failure. SSLEngine will close inbound", session.getSessionId());
                        sslEngine.closeInbound();
                        break outer;

                    default: // BUFFER_UNDERFLOW
                        throw new IOException(StringUtils.replace("Session {} handshake exception. status -> {}", session.getSessionId(), result.getStatus()));
                }
            }
        }
    }

    protected void merge(ByteBuffer now) {
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

    protected ByteBuffer getOutAppBuffer() {
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
    protected SSLEngineResult.HandshakeStatus doTasks() {
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

    @Override
    public String applicationProtocol() {
        String protocol = applicationProtocolSelector.applicationProtocol();
        log.debug("selected protocol -> {}", protocol);
        return protocol;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    protected ByteBuffer splitBuffer(int netSize) {
        ByteBuffer buf = inNetBuffer.duplicate();
        if (buf.remaining() <= netSize) {
            return buf;
        } else {
            ByteBuffer splitBuf = ByteBuffer.allocate(netSize);
            byte[] data = new byte[netSize];
            buf.get(data);
            splitBuf.put(data).flip();
            return splitBuf;
        }
    }

    abstract protected SSLEngineResult unwrap(ByteBuffer input) throws IOException;
    abstract protected SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws IOException;

    /**
     * This method is used to decrypt data, it implied do handshake
     *
     * @param receiveBuffer Encrypted message
     * @return plaintext
     * @throws IOException sslEngine error during data read
     */
    @Override
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

        needIO:
        while (true) {
            ByteBuffer buf = splitBuffer(netSize);
            SSLEngineResult result = unwrap(buf);

            if (log.isDebugEnabled()) {
                log.debug("Session {} read data result -> {}, inNetRemain -> {}", session.getSessionId(), result.toString(), inNetBuffer.remaining());
            }

            switch (result.getStatus()) {
                case BUFFER_OVERFLOW: {
                    // Reset the application buffer size.
                    int appSize = sslEngine.getSession().getApplicationBufferSize();
                    ByteBuffer b = ByteBuffer.allocate(appSize + outAppBuffer.position());
                    outAppBuffer.flip();
                    b.put(outAppBuffer);
                    outAppBuffer = b;
                    // retry the operation.
                }
                break;

                case BUFFER_UNDERFLOW: {
                    if (inNetBuffer.remaining() >= netSize) {
                        break; // retry the operation.
                    } else {
                        break needIO;
                    }
                }

                case OK: {
                    if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                        doTasks();
                    }
                    if (inNetBuffer.hasRemaining()) {
                        break; // retry the operation.
                    } else {
                        break needIO;
                    }
                }

                case CLOSED: {
                    log.info("Session {} read data failure. SSLEngine will close inbound", session.getSessionId());
                    sslEngine.closeInbound();
                }
                break needIO;

                default:
                    throw new IOException(StringUtils.replace("Session {} SSLEngine read data exception. status -> {}",
                            session.getSessionId(), result.getStatus()));
            }
        }

        return getOutAppBuffer();
    }

    @Override
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
    @Override
    public int write(ByteBuffer outputBuffer, Callback callback) throws IOException {
        if (!initialHSComplete)
            throw new IllegalStateException("The initial handshake is not complete.");

        int ret = 0;
        if (!outputBuffer.hasRemaining()) {
            return ret;
        }

        final int remain = outputBuffer.remaining();

        outer:
        while (ret < remain) {
            ByteBuffer writeBuf = ByteBuffer.allocate(writeBufferSize);

            wrap:
            while (true) {
                SSLEngineResult result = wrap(outputBuffer, writeBuf);
                ret += result.bytesConsumed();

                switch (result.getStatus()) {
                    case OK: {
                        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                            doTasks();
                        }

                        writeBuf.flip();
                        session.write(writeBuf, callback);
                    }
                    break wrap;

                    case BUFFER_OVERFLOW: {
                        int netSize = sslEngine.getSession().getPacketBufferSize();
                        ByteBuffer b = ByteBuffer.allocate(writeBuf.position() + netSize);
                        writeBuf.flip();
                        b.put(writeBuf);
                        writeBuf = b;
                    }
                    break; // retry the operation.

                    case CLOSED: {
                        log.info("Session {} write data failure. SSLEngine will close", session.getSessionId());
                        sslEngine.closeOutbound();
                    }
                    break outer;

                    default:
                        throw new IOException(StringUtils.replace("Session {} SSLEngine writes data exception. status -> {}",
                                session.getSessionId(), result.getStatus()));
                }
            }
        }

        return ret;
    }

    protected class FileBufferReaderHandler implements BufferReaderHandler {

        private final long len;

        private FileBufferReaderHandler(long len) {
            this.len = len;
        }

        @Override
        public void readBuffer(ByteBuffer buf, CountingCallback countingCallback, long count) {
            log.debug("write file,  count: {} , length: {}", count, len);
            try {
                write(buf, countingCallback);
            } catch (Throwable e) {
                log.error("ssl session writing error", e);
            }
        }

    }

    @Override
    public long transferFileRegion(FileRegion file, Callback callback) throws IOException {
        long ret = 0;
        try (FileRegion fileRegion = file) {
            fileRegion.transferTo(callback, new FileBufferReaderHandler(file.getLength()));
        }
        return ret;
    }

    @Override
    public boolean isHandshakeFinished() {
        return initialHSComplete;
    }

    @Override
    public boolean isClientMode() {
        return sslEngine.getUseClientMode();
    }
}
