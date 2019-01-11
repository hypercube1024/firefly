package com.fireflysource.net.tcp.secure;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.Result;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.tcp.TcpConnection;
import com.fireflysource.net.tcp.secure.exception.SecureNetException;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.fireflysource.common.sys.Result.discard;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractSecureEngine implements SecureEngine {

    protected static final LazyLogger LOG = SystemLogger.create(AbstractSecureEngine.class);

    protected static final ByteBuffer hsBuffer = ByteBuffer.allocateDirect(0);
    protected static final ByteBuffer emptyBuf = ByteBuffer.allocate(0);

    protected final TcpConnection tcpConnection;
    protected final SSLEngine sslEngine;
    protected final ApplicationProtocolSelector applicationProtocolSelector;

    protected ByteBuffer receivedPacketBuf;
    protected ByteBuffer receivedAppBuf;

    protected AtomicBoolean closed = new AtomicBoolean(false);
    protected SSLEngineResult.HandshakeStatus initialHSStatus;
    protected AtomicBoolean initialHSComplete = new AtomicBoolean(false);
    protected Consumer<Result<Void>> handshakeResult;

    public AbstractSecureEngine(TcpConnection tcpConnection, SSLEngine sslEngine,
                                ApplicationProtocolSelector applicationProtocolSelector) {
        this.tcpConnection = tcpConnection;
        this.sslEngine = sslEngine;
        this.applicationProtocolSelector = applicationProtocolSelector;

        receivedAppBuf = newBuffer(sslEngine.getSession().getApplicationBufferSize());
    }

    @Override
    public void beginHandshake(Consumer<Result<Void>> result) {
        handshakeResult = result;
        // start tls
        try {
            this.sslEngine.beginHandshake();
            initialHSStatus = sslEngine.getHandshakeStatus();
            if (sslEngine.getUseClientMode()) {
                doHandshakeResponse();
            }
        } catch (IOException | SecureNetException e) {
            result.accept(new Result<>(false, null, e));
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
        if (tcpConnection.isClosed()) {
            close();
            return false;
        }

        if (initialHSComplete.get()) {
            return true;
        }

        switch (initialHSStatus) {
            case NOT_HANDSHAKING:
            case FINISHED: {
                handshakeComplete();
                return initialHSComplete.get();
            }

            case NEED_UNWRAP:
                doHandshakeReceive(receiveBuffer);
                if (initialHSStatus != SSLEngineResult.HandshakeStatus.NEED_WRAP)
                    break;

            case NEED_WRAP:
                doHandshakeResponse();
                break;

            default: { // NEED_TASK
                SecureNetException e = new SecureNetException("Invalid Handshaking State" + initialHSStatus);
                if (handshakeResult != null) {
                    handshakeResult.accept(new Result<>(false, null, e));
                }
                throw e;
            }
        }
        return initialHSComplete.get();
    }

    protected void doHandshakeReceive(ByteBuffer receiveBuffer) throws IOException {
        merge(receiveBuffer);
        needIO:
        while (initialHSStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {

            unwrap:
            while (true) {
                SSLEngineResult result = unwrap();
                initialHSStatus = result.getHandshakeStatus();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connection {} handshake result -> {}, initialHSStatus -> {}, inNetRemain -> {}", tcpConnection.getId(), result.toString(), initialHSStatus, receivedPacketBuf.remaining());
                }

                switch (result.getStatus()) {
                    case OK: {
                        switch (initialHSStatus) {
                            case NEED_TASK:
                                initialHSStatus = doTasks();
                                break unwrap;
                            case NOT_HANDSHAKING:
                            case FINISHED:
                                handshakeComplete();
                                break needIO;
                            default:
                                break unwrap;
                        }
                    }

                    case BUFFER_UNDERFLOW: {
                        switch (initialHSStatus) {
                            case NOT_HANDSHAKING:
                            case FINISHED:
                                handshakeComplete();
                                break needIO;
                        }

                        int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
                        if (receivedPacketBuf.remaining() >= packetBufferSize) {
                            break; // retry the operation.
                        } else {
                            break needIO;
                        }
                    }

                    case BUFFER_OVERFLOW: {
                        resizeAppBuffer();
                        // retry the operation.
                    }
                    break;

                    case CLOSED: {
                        LOG.info("Connection {} handshake failure. SSLEngine will close inbound", tcpConnection.getId());
                        closeInbound();
                    }
                    break needIO;

                    default:
                        throw new SecureNetException(StringUtils.replace("Connection {} handshake exception. status -> {}", tcpConnection.getId(), result.getStatus()));

                }
            }
        }
    }

    protected void handshakeComplete() {
        if (initialHSComplete.compareAndSet(false, true)) {
            LOG.info("Connection {} handshake success. The application protocol is {}", tcpConnection.getId(), getApplicationProtocol());
            handshakeResult.accept(new Result<>(true, null, null));
        }
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connection {} handshake response, init: {} | ret: {} | complete: {} ",
                            tcpConnection.getId(), initialHSStatus, result.getStatus(), initialHSComplete.get());
                }
                switch (result.getStatus()) {
                    case OK: {
                        packetBuffer.flip();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Connection {} handshake response {} bytes", tcpConnection.getId(), packetBuffer.remaining());
                        }
                        switch (initialHSStatus) {
                            case NEED_TASK: {
                                initialHSStatus = doTasks();
                                if (packetBuffer.hasRemaining()) {
                                    tcpConnection.write(packetBuffer, discard());
                                }
                            }
                            break;
                            case FINISHED: {
                                if (packetBuffer.hasRemaining()) {
                                    tcpConnection.write(packetBuffer, r -> {
                                        if (r.isSuccess()) {
                                            handshakeComplete();
                                        } else {
                                            handshakeResult.accept(new Result<>(false, null, r.getThrowable()));
                                        }
                                    });
                                } else {
                                    handshakeComplete();
                                }
                            }
                            break;
                            default: {
                                if (packetBuffer.hasRemaining()) {
                                    tcpConnection.write(packetBuffer, discard());
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
                        LOG.info("Connection {} handshake failure. SSLEngine will close inbound", tcpConnection.getId());
                        packetBuffer.flip();
                        if (packetBuffer.hasRemaining()) {
                            tcpConnection.write(packetBuffer, discard());
                        }
                        closeOutbound();
                        break outer;

                    default: // BUFFER_UNDERFLOW
                        throw new SecureNetException(StringUtils.replace("Connection {} handshake exception. status -> {}", tcpConnection.getId(), result.getStatus()));
                }
            }
        }
    }

    protected void resizeAppBuffer() {
        int applicationBufferSize = sslEngine.getSession().getApplicationBufferSize();
        ByteBuffer b = newBuffer(receivedAppBuf.position() + applicationBufferSize);
        receivedAppBuf.flip();
        b.put(receivedAppBuf);
        receivedAppBuf = b;
    }

    protected void merge(ByteBuffer now) {
        if (!now.hasRemaining()) {
            return;
        }

        if (receivedPacketBuf != null) {
            if (receivedPacketBuf.hasRemaining()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connection {} read data, merge buffer -> {}, {}", tcpConnection.getId(),
                            receivedPacketBuf.remaining(), now.remaining());
                }
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
        LOG.debug("Connection {} read data, get app buf -> {}, {}", tcpConnection.getId(), receivedAppBuf.position(), receivedAppBuf.limit());
        if (receivedAppBuf.hasRemaining()) {
            ByteBuffer buf = newBuffer(receivedAppBuf.remaining());
            buf.put(receivedAppBuf).flip();
            receivedAppBuf = newBuffer(sslEngine.getSession().getApplicationBufferSize());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Connection {} unwrap, app buffer -> {}", tcpConnection.getId(), buf.remaining());
            }
            return buf;
        } else {
            return emptyBuf;
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
        if (closed.compareAndSet(false, true)) {
            closeOutbound();
        }
    }

    protected void closeInbound() {
        try {
            sslEngine.closeInbound();
        } catch (SSLException e) {
            LOG.warn("close inbound exception", e);
        } finally {
            try {
                tcpConnection.close();
            } catch (IOException ignored) {
            }
        }
    }

    protected void closeOutbound() throws IOException {
        sslEngine.closeOutbound();
        tcpConnection.close();
    }

    @Override
    public String getApplicationProtocol() {
        String protocol = applicationProtocolSelector.getApplicationProtocol();
        LOG.debug("selected protocol -> {}", protocol);
        return protocol;
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return applicationProtocolSelector.getSupportedApplicationProtocols();
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

    protected SSLEngineResult unwrap() throws IOException {
        int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
        //split net buffer when the net buffer remaining great than the net size
        ByteBuffer buf = splitBuffer(packetBufferSize);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Connection {} read data, buf -> {}, packet -> {}, appBuf -> {}",
                    tcpConnection.getId(), buf.remaining(), packetBufferSize, receivedAppBuf.remaining());
        }
        if (!receivedAppBuf.hasRemaining()) {
            resizeAppBuffer();
        }
        return unwrap(buf);
    }

    /**
     * This method is used to decrypt data, it implied do handshake
     *
     * @param receiveBuffer Encrypted message
     * @return plaintext
     * @throws IOException sslEngine error during data read
     */
    @Override
    public ByteBuffer decode(ByteBuffer receiveBuffer) throws IOException {
        if (!doHandshake(receiveBuffer))
            return emptyBuf;

        if (!initialHSComplete.get()) {
            throw new SecureNetException("The initial handshake is not complete.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Connection {} read data status -> {}, initialHSComplete -> {}",
                    tcpConnection.getId(), !tcpConnection.isClosed(), initialHSComplete.get());
        }

        merge(receiveBuffer);
        if (!receivedPacketBuf.hasRemaining()) {
            return emptyBuf;
        }

        needIO:
        while (true) {
            SSLEngineResult result = unwrap();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Connection {} read data result -> {}, receivedPacketBuf -> {}, appBufSize -> {}",
                        tcpConnection.getId(), result.toString().replace('\n', ' '),
                        receivedPacketBuf.remaining(), receivedAppBuf.remaining());
            }

            switch (result.getStatus()) {
                case BUFFER_OVERFLOW: {
                    resizeAppBuffer();
                    // retry the operation.
                }
                break;
                case BUFFER_UNDERFLOW: {
                    int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
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
                    LOG.info("Connection {} read data failure. SSLEngine will close inbound", tcpConnection.getId());
                    closeInbound();
                }
                break needIO;

                default:
                    throw new SecureNetException(StringUtils.replace("Connection {} SSLEngine read data exception. status -> {}",
                            tcpConnection.getId(), result.getStatus()));
            }
        }

        return getReceivedAppBuf();
    }


    /**
     * This method is used to encrypt and flush to socket channel
     *
     * @param outAppBuf Plaintext message
     * @return The encrypted data.
     * @throws IOException sslEngine error during data write
     */
    @Override
    public List<ByteBuffer> encode(ByteBuffer outAppBuf) throws IOException {
        if (!initialHSComplete.get()) {
            throw new SecureNetException("The initial handshake is not complete.");
        }

        int ret = 0;
        if (!outAppBuf.hasRemaining()) {
            return Collections.emptyList();
        }

        final int remain = outAppBuf.remaining();
        int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
        List<ByteBuffer> pocketBuffers = new ArrayList<>();
//        boolean closeOutput = false;

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
                        LOG.info("Connection {} SSLEngine will close", tcpConnection.getId());
                        packetBuffer.flip();
                        if (packetBuffer.hasRemaining()) {
                            pocketBuffers.add(packetBuffer);
                        }
                        if (pocketBuffers.isEmpty()) {
                            closeOutbound();
                        }
//                        closeOutput = true;
                    }
                    break outer;

                    default: {
                        throw new SecureNetException(StringUtils.replace("Connection {} SSLEngine writes data exception. status -> {}",
                                tcpConnection.getId(), result.getStatus()));
                    }
                }
            }
        }

//        if (closeOutput) {
//            closeOutbound();
//        }
        return pocketBuffers;
    }


    @Override
    public boolean isHandshakeComplete() {
        return initialHSComplete.get();
    }

    @Override
    public boolean isClientMode() {
        return sslEngine.getUseClientMode();
    }
}
