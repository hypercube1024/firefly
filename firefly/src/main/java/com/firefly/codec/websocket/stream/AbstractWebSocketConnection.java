package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.decode.Parser;
import com.firefly.codec.websocket.encode.Generator;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.*;
import com.firefly.codec.websocket.stream.IOState.ConnectionStateListener;
import com.firefly.net.Connection;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides the implementation of {@link WebsocketConnection} within the framework of the new {@link Connection} framework of the Firefly-nettools
 */
public abstract class AbstractWebSocketConnection implements WebsocketConnection, ConnectionStateListener {

    private class Flusher extends FrameFlusher {
        private Flusher(Generator generator, com.firefly.net.Session endpoint) {
            super(generator, endpoint, getPolicy().getMaxBinaryMessageBufferSize(), 8);
        }

        @Override
        public void onCompleteFailure(Throwable failure) {
            super.onCompleteFailure(failure);
            notifyError(failure);
            if (ioState.wasAbnormalClose()) {
                LOG.warn("Flush failure", failure);
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Write flush failure", failure);
            }
            ioState.onWriteFailure(failure);
            disconnect();
        }
    }

    public static class Stats {
        private AtomicLong countFillInterestedEvents = new AtomicLong(0);
        private AtomicLong countOnFillableEvents = new AtomicLong(0);
        private AtomicLong countFillableErrors = new AtomicLong(0);

        public long getFillableErrorCount() {
            return countFillableErrors.get();
        }

        public long getFillInterestedCount() {
            return countFillInterestedEvents.get();
        }

        public long getOnFillableCount() {
            return countOnFillableEvents.get();
        }
    }

    private enum ReadMode {
        PARSE,
        DISCARD,
        EOF
    }

    private static Logger LOG = LoggerFactory.getLogger("firefly-system");
    private static final AtomicLong ID_GEN = new AtomicLong(0);

    /**
     * Minimum size of a buffer is the determined to be what would be the maximum framing header size (not including payload)
     */
    private static final int MIN_BUFFER_SIZE = Generator.MAX_HEADER_LENGTH;

    private final Scheduler scheduler;
    private final Generator generator;
    private final Parser parser;
    private final WebSocketPolicy policy;
    private final AtomicBoolean suspendToken;
    private final FrameFlusher flusher;
    private final String id;
    private WebSocketSession session;
    private List<ExtensionConfig> extensions;
    private boolean isFilling;
    private ByteBuffer prefillBuffer;
    private ReadMode readMode = ReadMode.PARSE;
    private IOState ioState;
    private Stats stats = new Stats();

    public AbstractWebSocketConnection(com.firefly.net.Session endp, Executor executor, Scheduler scheduler,
                                       WebSocketPolicy policy) {

        this.id = Long.toString(ID_GEN.incrementAndGet());
        this.policy = policy;
        this.generator = new Generator(policy);
        this.parser = new Parser(policy);
        this.scheduler = scheduler;
        this.extensions = new ArrayList<>();
        this.suspendToken = new AtomicBoolean(false);
        this.ioState = new IOState();
        this.ioState.addListener(this);
        this.flusher = new Flusher(generator, endp);
//        this.setInputBufferSize(policy.getInputBufferSize());
        this.setMaxIdleTimeout(policy.getIdleTimeout());
    }

    @Override
    public Executor getExecutor() {
        return null;
    }

    @Override
    public void onLocalClose(CloseInfo close) {
        if (LOG.isDebugEnabled())
            LOG.debug("Local Close Confirmed {}", close);

        if (close.isAbnormal()) {
            ioState.onAbnormalClose(close);
        } else {
            ioState.onCloseLocal(close);
        }
    }

    @Override
    public void setSession(WebSocketSession session) {
        this.session = session;
    }


    public boolean onIdleExpired() {
        // TODO: handle closing handshake (see HTTP2Connection).
        return false;
    }

    @Override
    public void close() {
        session.close();
    }

    @Override
    public void disconnect() {
        if (LOG.isDebugEnabled())
            LOG.debug("{} disconnect()", policy.getBehavior());
        flusher.terminate(new EOFException("Disconnected"), false);
        com.firefly.net.Session endPoint = getEndPoint();
        // We need to gently close first, to allow
        // SSL close alerts to be sent by Firefly
        endPoint.shutdownOutput();
        endPoint.close();
    }

    public void fillInterested() {
        stats.countFillInterestedEvents.incrementAndGet();
    }

    /**
     * Get the list of extensions in use.
     * <p>
     * This list is negotiated during the WebSocket Upgrade Request/Response handshake.
     *
     * @return the list of negotiated extensions in use.
     */
    public List<ExtensionConfig> getExtensions() {
        return extensions;
    }

    public Generator getGenerator() {
        return generator;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getIdleTimeout() {
        return getEndPoint().getIdleTimeout();
    }

    @Override
    public IOState getIOState() {
        return ioState;
    }

    @Override
    public long getMaxIdleTimeout() {
        return getEndPoint().getIdleTimeout();
    }

    public Parser getParser() {
        return parser;
    }

    @Override
    public WebSocketPolicy getPolicy() {
        return this.policy;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return getEndPoint().getRemoteAddress();
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Stats getStats() {
        return stats;
    }

    @Override
    public boolean isOpen() {
        return getEndPoint().isOpen();
    }

    @Override
    public boolean isReading() {
        return isFilling;
    }

    /**
     * Physical connection disconnect.
     * <p>
     * Not related to WebSocket close handshake.
     */
    public void onClose() {
        if (LOG.isDebugEnabled())
            LOG.debug("{} onClose()", policy.getBehavior());
        ioState.onDisconnected();
    }

    @Override
    public void onConnectionStateChange(ConnectionState state) {
        if (LOG.isDebugEnabled())
            LOG.debug("{} Connection State Change: {}", policy.getBehavior(), state);

        switch (state) {
            case OPEN:
                if (BufferUtils.hasContent(prefillBuffer)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Parsing Upgrade prefill buffer ({} remaining)", prefillBuffer.remaining());
                    }
                    parser.parse(prefillBuffer);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("OPEN: normal fillInterested");
                }
                // TODO: investigate what happens if a failure occurs during prefill, and an attempt to write close fails,
                // should a fill interested occur? or just a quick disconnect?
                fillInterested();
                break;
            case CLOSED:
                if (LOG.isDebugEnabled())
                    LOG.debug("CLOSED - wasAbnormalClose: {}", ioState.wasAbnormalClose());
                if (ioState.wasAbnormalClose()) {
                    // Fire out a close frame, indicating abnormal shutdown, then disconnect
                    session.close(StatusCode.SHUTDOWN, "Abnormal Close - " + ioState.getCloseInfo().getReason());
                } else {
                    // Just disconnect
                    this.disconnect();
                }
                break;
            case CLOSING:
                if (LOG.isDebugEnabled())
                    LOG.debug("CLOSING - wasRemoteCloseInitiated: {}", ioState.wasRemoteCloseInitiated());

                // First occurrence of .onCloseLocal or .onCloseRemote use
                if (ioState.wasRemoteCloseInitiated()) {
                    CloseInfo close = ioState.getCloseInfo();
                    session.close(close.getStatusCode(), close.getReason());
                }
            default:
                break;
        }
    }

//    public void onFillable()
//    {
//        if (LOG.isDebugEnabled())
//            LOG.debug("{} onFillable()",policy.getBehavior());
//        stats.countOnFillableEvents.incrementAndGet();
//
//        ByteBuffer buffer = BufferUtils.allocate(getInputBufferSize());
//
//        isFilling = true;
//
//        if(readMode == ReadMode.PARSE)
//        {
//            readMode = readParse(buffer);
//        }
//        else
//        {
//            readMode = readDiscard(buffer);
//        }
//
//        if ((readMode != ReadMode.EOF) && (suspendToken.get() == false))
//        {
//            fillInterested();
//        }
//        else
//        {
//            isFilling = false;
//        }
//    }

    protected void onFillInterestedFailed(Throwable cause) {
        LOG.warn("Fill interested failure", cause);
        stats.countFillInterestedEvents.incrementAndGet();
    }

    /**
     * Extra bytes from the initial HTTP upgrade that need to
     * be processed by the websocket parser before starting
     * to read bytes from the connection
     *
     * @param prefilled the bytes of prefilled content encountered during upgrade
     */
    protected void setInitialBuffer(ByteBuffer prefilled) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("set Initial Buffer - {}", BufferUtils.toDetailString(prefilled));
        }
        prefillBuffer = prefilled;
    }

    private void notifyError(Throwable t) {
        getParser().getIncomingFramesHandler().incomingError(t);
    }

    public void onOpen() {
        if (LOG.isDebugEnabled())
            LOG.debug("[{}] {}.onOpened()", policy.getBehavior(), this.getClass().getSimpleName());
        this.ioState.onOpened();
    }

    /**
     * Event for no activity on connection (read or write)
     */
    protected boolean onReadTimeout() {
        IOState state = getIOState();
        ConnectionState cstate = state.getConnectionState();
        if (LOG.isDebugEnabled())
            LOG.debug("{} Read Timeout - {}", policy.getBehavior(), cstate);

        if (cstate == ConnectionState.CLOSED) {
            if (LOG.isDebugEnabled())
                LOG.debug("onReadTimeout - Connection Already CLOSED");
            // close already completed, extra timeouts not relevant
            // allow underlying connection and endpoint to disconnect on its own
            return true;
        }

        try {
            notifyError(new SocketTimeoutException("Timeout on Read"));
        } finally {
            // This is an Abnormal Close condition
            session.close(StatusCode.SHUTDOWN, "Idle Timeout");
        }

        return false;
    }

    /**
     * Frame from API, User, or Internal implementation destined for network.
     */
    @Override
    public void outgoingFrame(Frame frame, WriteCallback callback, BatchMode batchMode) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("outgoingFrame({}, {})", frame, callback);
        }

        flusher.enqueue(frame, callback, batchMode);
    }

    private ReadMode readDiscard(ByteBuffer buffer) {
//        com.firefly.net.Session endPoint = getEndPoint();
//        try
//        {
//            while (true)
//            {
//                int filled = endPoint.fill(buffer);
//                if (filled == 0)
//                {
//                    return ReadMode.DISCARD;
//                }
//                else if (filled < 0)
//                {
//                    if (LOG.isDebugEnabled())
//                        LOG.debug("read - EOF Reached (remote: {})",getRemoteAddress());
//                    return ReadMode.EOF;
//                }
//                else
//                {
//                    if (LOG.isDebugEnabled())
//                        LOG.debug("Discarded {} bytes - {}",filled,BufferUtils.toDetailString(buffer));
//                }
//            }
//        }
//        catch (IOException e)
//        {
//            LOG.warn("", e);
//            return ReadMode.EOF;
//        }
//        catch (Throwable t)
//        {
//            LOG.ignore(t);
//            return ReadMode.DISCARD;
//        }
        return null;
    }

    private ReadMode readParse(ByteBuffer buffer) {
//        com.firefly.net.Session endPoint = getEndPoint();
//        try
//        {
//            // Process the content from the Endpoint next
//            while(true)  // TODO: should this honor the LogicalConnection.suspend() ?
//            {
//                int filled = endPoint.fill(buffer);
//                if (filled < 0)
//                {
//                    LOG.debug("read - EOF Reached (remote: {})",getRemoteAddress());
//                    ioState.onReadFailure(new EOFException("Remote Read EOF"));
//                    return ReadMode.EOF;
//                }
//                else if (filled == 0)
//                {
//                    // Done reading, wait for next onFillable
//                    return ReadMode.PARSE;
//                }
//
//                if (LOG.isDebugEnabled())
//                {
//                    LOG.debug("Filled {} bytes - {}",filled,BufferUtils.toDetailString(buffer));
//                }
//                parser.parse(buffer);
//            }
//        }
//        catch (IOException e)
//        {
//            LOG.warn(e);
//            session.notifyError(e);
//            session.abort(StatusCode.PROTOCOL,e.getMessage());
//            return ReadMode.DISCARD;
//        }
//        catch (CloseException e)
//        {
//            LOG.debug(e);
//            session.notifyError(e);
//            session.close(e.getStatusCode(),e.getMessage());
//            return ReadMode.DISCARD;
//        }
//        catch (Throwable t)
//        {
//            LOG.warn(t);
//            session.abort(StatusCode.ABNORMAL,t.getMessage());
//            // TODO: should probably only switch to discard if a non-ws-endpoint error
//            return ReadMode.DISCARD;
//        }
        return null;
    }

    @Override
    public void resume() {
        if (suspendToken.getAndSet(false)) {
            if (!isReading()) {
                fillInterested();
            }
        }
    }

    /**
     * Get the list of extensions in use.
     * <p>
     * This list is negotiated during the WebSocket Upgrade Request/Response handshake.
     *
     * @param extensions the list of negotiated extensions in use.
     */
    public void setExtensions(List<ExtensionConfig> extensions) {
        this.extensions = extensions;
    }

//    @Override
//    public void setInputBufferSize(int inputBufferSize)
//    {
//        if (inputBufferSize < MIN_BUFFER_SIZE)
//        {
//            throw new IllegalArgumentException("Cannot have buffer size less than " + MIN_BUFFER_SIZE);
//        }
//        super.setInputBufferSize(inputBufferSize);
//    }
//
//    @Override
//    public void setMaxIdleTimeout(long ms)
//    {
//        getEndPoint().setIdleTimeout(ms);
//    }

    @Override
    public SuspendToken suspend() {
        suspendToken.set(true);
        return this;
    }

    public String toConnectionString() {
        return String.format("%s@%x[ios=%s,f=%s,g=%s,p=%s]",
                getClass().getSimpleName(),
                hashCode(),
                ioState, flusher, generator, parser);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        com.firefly.net.Session endp = getEndPoint();
        if (endp != null) {
            result = prime * result + endp.getLocalAddress().hashCode();
            result = prime * result + endp.getRemoteAddress().hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractWebSocketConnection other = (AbstractWebSocketConnection) obj;
        com.firefly.net.Session endp = getEndPoint();
        com.firefly.net.Session otherEndp = other.getEndPoint();
        if (endp == null) {
            if (otherEndp != null)
                return false;
        } else if (!endp.equals(otherEndp))
            return false;
        return true;
    }

    /**
     * Extra bytes from the initial HTTP upgrade that need to
     * be processed by the websocket parser before starting
     * to read bytes from the connection
     */
    public void onUpgradeTo(ByteBuffer prefilled) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onUpgradeTo({})", BufferUtils.toDetailString(prefilled));
        }

        setInitialBuffer(prefilled);
    }

    public com.firefly.net.Session getEndPoint() {
        return null;
    }
}
