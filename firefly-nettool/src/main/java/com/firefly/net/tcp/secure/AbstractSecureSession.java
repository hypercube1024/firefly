package com.firefly.net.tcp.secure;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.net.SecureSession;
import com.firefly.net.SecureSessionHandshakeListener;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.net.exception.SecureNetException;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.io.BufferReaderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractSecureSession implements SecureSession {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected static final ByteBuffer hsBuffer = ByteBuffer.allocateDirect(0);

    protected final Session session;
    protected final SSLEngine sslEngine;
    protected final ApplicationProtocolSelector applicationProtocolSelector;
    protected final SecureSessionHandshakeListener handshakeListener;

    protected ByteBuffer receivedPacketBuf;
    protected ByteBuffer receivedAppBuf;

    protected volatile boolean closed = false;
    protected SSLEngineResult.HandshakeStatus initialHSStatus;
    protected boolean initialHSComplete;

    public AbstractSecureSession(Session session, SSLEngine sslEngine,
                                 ApplicationProtocolSelector applicationProtocolSelector,
                                 SecureSessionHandshakeListener handshakeListener) throws IOException {
        this.session = session;
        this.sslEngine = sslEngine;
        this.applicationProtocolSelector = applicationProtocolSelector;
        this.handshakeListener = handshakeListener;

        receivedAppBuf = newBuffer(sslEngine.getSession().getApplicationBufferSize());
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
     * @throws IOException The I/O exception
     */
    protected boolean doHandshake(ByteBuffer receiveBuffer) throws IOException {
        if (!session.isOpen()) {
            close();
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
                throw new SecureNetException("Invalid Handshaking State" + initialHSStatus);
        }
        return initialHSComplete;
    }

    protected void doHandshakeReceive(ByteBuffer receiveBuffer) throws IOException {
        merge(receiveBuffer);
        int packetBufferSize = sslEngine.getSession().getPacketBufferSize();

        needIO:
        while (initialHSStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {

            unwrap:
            while (true) {
                ByteBuffer buf = splitBuffer(packetBufferSize);
                SSLEngineResult result = unwrap(buf);
                initialHSStatus = result.getHandshakeStatus();

                if (log.isDebugEnabled()) {
                    log.debug("Session {} handshake result -> {}, initialHSStatus -> {}, inNetRemain -> {}", session.getSessionId(), result.toString(), initialHSStatus, receivedPacketBuf.remaining());
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

                        if (receivedPacketBuf.remaining() >= packetBufferSize) {
                            break; // retry the operation.
                        } else {
                            break needIO;
                        }
                    }

                    case BUFFER_OVERFLOW: {
                        // Reset the application buffer size.
                        ByteBuffer b = newBuffer(receivedAppBuf.position() + sslEngine.getSession().getApplicationBufferSize());
                        receivedAppBuf.flip();
                        b.put(receivedAppBuf);
                        receivedAppBuf = b;
                    }
                    break; // retry the operation.

                    case CLOSED: {
                        log.info("Session {} handshake failure. SSLEngine will close inbound", session.getSessionId());
                        closeInbound();
                    }
                    break needIO;

                    default:
                        throw new SecureNetException(StringUtils.replace("Session {} handshake exception. status -> {}", session.getSessionId(), result.getStatus()));

                }
            }
        }
    }

    protected void handshakeFinish() {
        log.info("Session {} handshake success. The application protocol is {}", session.getSessionId(), getApplicationProtocol());
        initialHSComplete = true;
        handshakeListener.complete(this);
    }


    protected void doHandshakeResponse() throws IOException {

        outer:
        while (initialHSStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            SSLEngineResult result;
            ByteBuffer packetBuffer = newBuffer(sslEngine.getSession().getPacketBufferSize());

            wrap:
            while (true) {
                result = sslEngine.wrap(hsBuffer, packetBuffer);
                initialHSStatus = result.getHandshakeStatus();
                if (log.isDebugEnabled()) {
                    log.debug("session {} handshake response, init: {} | ret: {} | complete: {} ",
                            session.getSessionId(), initialHSStatus, result.getStatus(), initialHSComplete);
                }
                switch (result.getStatus()) {
                    case OK: {
                        packetBuffer.flip();
                        if (log.isDebugEnabled()) {
                            log.debug("session {} handshake response {} bytes", session.getSessionId(), packetBuffer.remaining());
                        }
                        switch (initialHSStatus) {
                            case NEED_TASK: {
                                initialHSStatus = doTasks();
                                if (packetBuffer.hasRemaining()) {
                                    session.write(packetBuffer, Callback.NOOP);
                                }
                            }
                            break;
                            case FINISHED: {
                                if (packetBuffer.hasRemaining()) {
                                    session.write(packetBuffer, new Callback() {
                                        public void succeeded() {
                                            handshakeFinish();
                                        }
                                    });
                                } else {
                                    handshakeFinish();
                                }
                            }
                            break;
                            default: {
                                if (packetBuffer.hasRemaining()) {
                                    session.write(packetBuffer, Callback.NOOP);
                                }
                            }
                        }
                    }
                    break wrap;

                    case BUFFER_OVERFLOW:
                        ByteBuffer b = newBuffer(packetBuffer.position() + sslEngine.getSession().getPacketBufferSize());
                        packetBuffer.flip();
                        b.put(packetBuffer);
                        packetBuffer = b;
                        break;

                    case CLOSED:
                        log.info("Session {} handshake failure. SSLEngine will close inbound", session.getSessionId());
                        packetBuffer.flip();
                        if (packetBuffer.hasRemaining()) {
                            session.write(packetBuffer, Callback.NOOP);
                        }
                        closeOutbound();
                        break outer;

                    default: // BUFFER_UNDERFLOW
                        throw new SecureNetException(StringUtils.replace("Session {} handshake exception. status -> {}", session.getSessionId(), result.getStatus()));
                }
            }
        }
    }

    protected void merge(ByteBuffer now) {
        if (!now.hasRemaining()) {
            return;
        }

        if (receivedPacketBuf != null) {
            if (receivedPacketBuf.hasRemaining()) {
                ByteBuffer ret = newBuffer(receivedPacketBuf.remaining() + now.remaining());
                ret.put(receivedPacketBuf).put(now).flip();
                receivedPacketBuf = ret;
            } else {
                receivedPacketBuf = now;
            }
        } else {
            receivedPacketBuf = now;
        }
    }

    protected ByteBuffer getReceivedAppBuf() {
        receivedAppBuf.flip();
        if (receivedAppBuf.hasRemaining()) {
            ByteBuffer buf = newBuffer(receivedAppBuf.remaining());
            buf.put(receivedAppBuf).flip();
            receivedAppBuf = newBuffer(sslEngine.getSession().getApplicationBufferSize());
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
    public synchronized void close() {
        if (!closed) {
            closeOutbound();
            closed = true;
        }
    }

    protected void closeInbound() {
        try {
            sslEngine.closeInbound();
        } catch (SSLException e) {
            log.warn("close inbound exception", e);
        } finally {
            session.shutdownInput();
        }
    }

    protected void closeOutbound() {
        sslEngine.closeOutbound();
        session.close();
    }

    @Override
    public String getApplicationProtocol() {
        String protocol = applicationProtocolSelector.getApplicationProtocol();
        log.debug("selected protocol -> {}", protocol);
        return protocol;
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return applicationProtocolSelector.getSupportedApplicationProtocols();
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    protected ByteBuffer splitBuffer(int netSize) {
        ByteBuffer buf = receivedPacketBuf.duplicate();
        if (buf.remaining() <= netSize) {
            return buf;
        } else {
            ByteBuffer splitBuf = newBuffer(netSize);
            byte[] data = new byte[netSize];
            buf.get(data);
            splitBuf.put(data).flip();
            return splitBuf;
        }
    }

    abstract protected SSLEngineResult unwrap(ByteBuffer input) throws IOException;

    abstract protected SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws IOException;

    abstract protected ByteBuffer newBuffer(int size);

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
        if (!receivedPacketBuf.hasRemaining()) {
            return null;
        }

        //split net buffer when the net buffer remaining great than the net size
        int packetBufferSize = sslEngine.getSession().getPacketBufferSize();

        needIO:
        while (true) {
            ByteBuffer buf = splitBuffer(packetBufferSize);
            if (log.isDebugEnabled()) {
                log.debug("Session {} read data, buf -> {}", session.getSessionId(), buf.remaining());
            }
            SSLEngineResult result = unwrap(buf);

            if (log.isDebugEnabled()) {
                log.debug("Session {} read data result -> {}, receivedPacketBuf -> {}, packetSize -> {}",
                        session.getSessionId(), result.toString().replace('\n', ' '),
                        receivedPacketBuf.remaining(), packetBufferSize);
            }

            switch (result.getStatus()) {
                case BUFFER_OVERFLOW: {
                    // Reset the application buffer size.
                    ByteBuffer b = newBuffer(receivedAppBuf.position() + sslEngine.getSession().getApplicationBufferSize());
                    receivedAppBuf.flip();
                    b.put(receivedAppBuf);
                    receivedAppBuf = b;
                    // retry the operation.
                }
                break;

                case BUFFER_UNDERFLOW: {
                    if (receivedPacketBuf.remaining() >= packetBufferSize) {
                        break; // retry the operation.
                    } else {
                        break needIO;
                    }
                }

                case OK: {
                    if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                        doTasks();
                    }
                    if (receivedPacketBuf.hasRemaining()) {
                        break; // retry the operation.
                    } else {
                        break needIO;
                    }
                }

                case CLOSED: {
                    log.info("Session {} read data failure. SSLEngine will close inbound", session.getSessionId());
                    closeInbound();
                }
                break needIO;

                default:
                    throw new SecureNetException(StringUtils.replace("Session {} SSLEngine read data exception. status -> {}",
                            session.getSessionId(), result.getStatus()));
            }
        }

        return getReceivedAppBuf();
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
     * @param outAppBuf Plaintext message
     * @return writen length
     * @throws IOException sslEngine error during data write
     */
    @Override
    public int write(ByteBuffer outAppBuf, Callback callback) throws IOException {
        if (!initialHSComplete) {
            IllegalStateException ex = new IllegalStateException("The initial handshake is not complete.");
            callback.failed(ex);
            throw ex;
        }

        int ret = 0;
        if (!outAppBuf.hasRemaining()) {
            callback.succeeded();
            return ret;
        }

        final int remain = outAppBuf.remaining();
        int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
        List<ByteBuffer> pocketBuffers = new ArrayList<>();
        boolean closeOutput = false;

        outer:
        while (ret < remain) {
            ByteBuffer packetBuffer = newBuffer(packetBufferSize);

            wrap:
            while (true) {
                SSLEngineResult result = wrap(outAppBuf, packetBuffer);
                ret += result.bytesConsumed();

                switch (result.getStatus()) {
                    case OK: {
                        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                            doTasks();
                        }

                        packetBuffer.flip();
                        if (packetBuffer.hasRemaining()) {
                            pocketBuffers.add(packetBuffer);
                        }
                    }
                    break wrap;

                    case BUFFER_OVERFLOW: {
                        packetBufferSize = sslEngine.getSession().getPacketBufferSize();
                        ByteBuffer b = newBuffer(packetBuffer.position() + packetBufferSize);
                        packetBuffer.flip();
                        b.put(packetBuffer);
                        packetBuffer = b;
                    }
                    break; // retry the operation.

                    case CLOSED: {
                        log.info("Session {} SSLEngine will close", session.getSessionId());
                        packetBuffer.flip();
                        if (packetBuffer.hasRemaining()) {
                            pocketBuffers.add(packetBuffer);
                        }
                        closeOutput = true;
                    }
                    break outer;

                    default: {
                        SecureNetException ex = new SecureNetException(StringUtils.replace("Session {} SSLEngine writes data exception. status -> {}", session.getSessionId(), result.getStatus()));
                        callback.failed(ex);
                        throw ex;
                    }
                }
            }
        }

        session.write(pocketBuffers, callback);
        if (closeOutput) {
            closeOutbound();
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
