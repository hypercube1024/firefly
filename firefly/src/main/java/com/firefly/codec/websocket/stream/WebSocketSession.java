package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.exception.CloseException;
import com.firefly.codec.websocket.exception.WebSocketException;
import com.firefly.codec.websocket.frame.CloseFrame;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.*;
import com.firefly.codec.websocket.model.extension.ExtensionFactory;
import com.firefly.codec.websocket.stream.IOState.ConnectionStateListener;
import com.firefly.net.Connection;
import com.firefly.utils.concurrent.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebSocketSession implements Session, RemoteEndpointFactory, IncomingFrames, ConnectionStateListener {
    public static class OnCloseLocalCallback implements WriteCallback {
        private final Callback callback;
        private final WebsocketConnection connection;
        private final CloseInfo close;

        public OnCloseLocalCallback(Callback callback, WebsocketConnection connection, CloseInfo close) {
            this.callback = callback;
            this.connection = connection;
            this.close = close;
        }

        @Override
        public void writeSuccess() {
            try {
                if (callback != null) {
                    callback.succeeded();
                }
            } finally {
                connection.onLocalClose(close);
            }
        }

        @Override
        public void writeFailed(Throwable x) {
            try {
                if (callback != null) {
                    callback.failed(x);
                }
            } finally {
                connection.onLocalClose(close);
            }
        }
    }

    public class DisconnectCallback implements Callback {
        @Override
        public void failed(Throwable x) {
            disconnect();
        }

        @Override
        public void succeeded() {
            disconnect();
        }
    }


    private static Logger LOG = LoggerFactory.getLogger("firefly-system");

    private final URI requestURI;
    private final WebsocketConnection connection;
    private final WebSocketEvent websocket;
    private final WebSocketPolicy policy;
    private final AtomicBoolean closed = new AtomicBoolean();
    private ClassLoader classLoader;
    private ExtensionFactory extensionFactory;
    private RemoteEndpointFactory remoteEndpointFactory;
    private String protocolVersion;
    private Map<String, String[]> parameterMap = new HashMap<>();
    private RemoteEndpoint remote;
    private IncomingFrames incomingHandler;
    private OutgoingFrames outgoingHandler;
    private UpgradeRequest upgradeRequest;
    private UpgradeResponse upgradeResponse;
    private CompletableFuture<Session> openFuture;

    public WebSocketSession(URI requestURI, WebSocketEvent websocket, WebsocketConnection connection) {
        Objects.requireNonNull(requestURI, "Request URI cannot be null");

        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.requestURI = requestURI;
        this.websocket = websocket;
        this.connection = connection;
        this.outgoingHandler = connection;
        this.incomingHandler = websocket;
        this.connection.getIOState().addListener(this);
        this.policy = websocket.getPolicy();

        this.connection.setSession(this);
    }

    /**
     * Aborts the active session abruptly.
     */
    public void abort(int statusCode, String reason) {
        close(new CloseInfo(statusCode, reason), new DisconnectCallback());
    }

    @Override
    public void close() {
        /* This is assumed to always be a NORMAL closure, no reason phrase */
        close(new CloseInfo(StatusCode.NORMAL), null);
    }

    @Override
    public void close(CloseStatus closeStatus) {
        close(new CloseInfo(closeStatus.getCode(), closeStatus.getPhrase()), null);
    }

    @Override
    public void close(int statusCode, String reason) {
        close(new CloseInfo(statusCode, reason), null);
    }

    /**
     * CLOSE Primary Entry Point.
     * <p>
     * <ul>
     * <li>atomically enqueue CLOSE frame + flip flag to reject more frames</li>
     * <li>setup CLOSE frame callback: must close flusher</li>
     * </ul>
     *
     * @param closeInfo the close details
     */
    private void close(CloseInfo closeInfo, Callback callback) {
        if (LOG.isDebugEnabled())
            LOG.debug("close({})", closeInfo);

        if (closed.compareAndSet(false, true)) {
            CloseFrame frame = closeInfo.asFrame();
            connection.outgoingFrame(frame, new OnCloseLocalCallback(callback, connection, closeInfo), BatchMode.OFF);
        }
    }

    /**
     * Harsh disconnect
     */
    @Override
    public void disconnect() {
        connection.disconnect();

        // notify of harsh disconnect
        notifyClose(StatusCode.NO_CLOSE, "Harsh disconnect");
    }

    protected void doStart() throws Exception {
        if (LOG.isDebugEnabled())
            LOG.debug("starting - {}", this);

        Iterator<RemoteEndpointFactory> iter = ServiceLoader.load(RemoteEndpointFactory.class).iterator();
        if (iter.hasNext())
            remoteEndpointFactory = iter.next();

        if (remoteEndpointFactory == null)
            remoteEndpointFactory = this;

        if (LOG.isDebugEnabled())
            LOG.debug("Using RemoteEndpointFactory: {}", remoteEndpointFactory);


    }


    protected void doStop() throws Exception {
        if (LOG.isDebugEnabled())
            LOG.debug("stopping - {}", this);
        try {
            close(StatusCode.SHUTDOWN, "Shutdown");
        } catch (Throwable t) {
            LOG.debug("During Connection Shutdown", t);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WebSocketSession other = (WebSocketSession) obj;
        if (connection == null) {
            if (other.connection != null) {
                return false;
            }
        } else if (!connection.equals(other.connection)) {
            return false;
        }
        return true;
    }

    public ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    public WebsocketConnection getConnection() {
        return connection;
    }

    public ExtensionFactory getExtensionFactory() {
        return extensionFactory;
    }

    /**
     * The idle timeout in milliseconds
     */
    @Override
    public long getIdleTimeout() {
        return connection.getMaxIdleTimeout();
    }

    public IncomingFrames getIncomingHandler() {
        return incomingHandler;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return connection.getLocalAddress();
    }

    public OutgoingFrames getOutgoingHandler() {
        return outgoingHandler;
    }

    @Override
    public WebSocketPolicy getPolicy() {
        return policy;
    }

    @Override
    public String getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public RemoteEndpoint getRemote() {
        if (LOG.isDebugEnabled())
            LOG.debug("[{}] {}.getRemote()", policy.getBehavior(), this.getClass().getSimpleName());
        ConnectionState state = connection.getIOState().getConnectionState();

        if ((state == ConnectionState.OPEN) || (state == ConnectionState.CONNECTED)) {
            return remote;
        }

        throw new WebSocketException("RemoteEndpoint unavailable, current state [" + state + "], expecting [OPEN or CONNECTED]");
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remote.getInetSocketAddress();
    }

    public URI getRequestURI() {
        return requestURI;
    }

    @Override
    public UpgradeRequest getUpgradeRequest() {
        return this.upgradeRequest;
    }

    @Override
    public UpgradeResponse getUpgradeResponse() {
        return this.upgradeResponse;
    }

    public WebSocketSession getWebSocketSession() {
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((connection == null) ? 0 : connection.hashCode());
        return result;
    }

    /**
     * Incoming Errors
     */
    @Override
    public void incomingError(Throwable t) {
        // Forward Errors to User WebSocket Object
        websocket.incomingError(t);
    }

    /**
     * Incoming Raw Frames from Parser
     */
    @Override
    public void incomingFrame(Frame frame) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            if (connection.getIOState().isInputAvailable()) {
                // Forward Frames Through Extension List
                incomingHandler.incomingFrame(frame);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public boolean isOpen() {
        if (this.connection == null) {
            return false;
        }
        return !closed.get() && this.connection.isOpen();
    }

    @Override
    public boolean isSecure() {
        if (upgradeRequest == null) {
            throw new IllegalStateException("No valid UpgradeRequest yet");
        }

        return "wss".equalsIgnoreCase(upgradeRequest.getRequestURI().getScheme());
    }

    public void notifyClose(int statusCode, String reason) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("notifyClose({},{})", statusCode, reason);
        }
        websocket.onClose(new CloseInfo(statusCode, reason));
    }

    public void notifyError(Throwable cause) {
        if (openFuture != null && !openFuture.isDone())
            openFuture.completeExceptionally(cause);
        incomingError(cause);
    }

    /**
     * Connection onClosed event
     *
     * @param connection the connection that was closed
     */
    public void onClosed(Connection connection) {
    }

    /**
     * Connection onOpen event
     *
     * @param connection the connection that was opened
     */
    public void onOpened(Connection connection) {
        if (LOG.isDebugEnabled())
            LOG.debug("[{}] {}.onOpened()", policy.getBehavior(), this.getClass().getSimpleName());
        open();
    }

    @Override
    public void onConnectionStateChange(ConnectionState state) {
        switch (state) {
            case CLOSED:
                IOState ioState = this.connection.getIOState();
                CloseInfo close = ioState.getCloseInfo();
                // confirmed close of local endpoint
                notifyClose(close.getStatusCode(), close.getReason());
                try {

                    // TODO
                } catch (Throwable t) {
                    LOG.warn("Connection close exception", t);
                }
                break;
            case CONNECTED:
                // notify session listeners
                try {
                    // TODO
                } catch (Throwable t) {
                    LOG.warn("WebSocket connected exception", t);
                }
                break;
        }
    }

    public WebSocketRemoteEndpoint newRemoteEndpoint(WebsocketConnection connection, OutgoingFrames outgoingFrames, BatchMode batchMode) {
        return new WebSocketRemoteEndpoint(connection, outgoingHandler, getBatchMode());
    }

    /**
     * Open/Activate the session
     */
    public void open() {
        if (LOG.isDebugEnabled())
            LOG.debug("[{}] {}.open()", policy.getBehavior(), this.getClass().getSimpleName());

        if (remote != null) {
            // already opened
            return;
        }

        try {
            // Upgrade success
            connection.getIOState().onConnected();

            // Connect remote
            remote = remoteEndpointFactory.newRemoteEndpoint(connection, outgoingHandler, getBatchMode());
            if (LOG.isDebugEnabled())
                LOG.debug("[{}] {}.open() remote={}", policy.getBehavior(), this.getClass().getSimpleName(), remote);

            // Open WebSocket
            websocket.openSession(this);

            // Open connection
            connection.getIOState().onOpened();

            if (LOG.isDebugEnabled()) {
                LOG.debug("open -> {}");
            }

            if (openFuture != null) {
                openFuture.complete(this);
            }
        } catch (CloseException ce) {
            LOG.warn("WebSocket open exception", ce);
            close(ce.getStatusCode(), ce.getMessage());
        } catch (Throwable t) {
            LOG.warn("WebSocket exception", t);
            // Exception on end-user WS-Endpoint.
            // Fast-fail & close connection with reason.
            int statusCode = StatusCode.SERVER_ERROR;
            if (policy.getBehavior() == WebSocketBehavior.CLIENT) {
                statusCode = StatusCode.POLICY_VIOLATION;
            }
            close(statusCode, t.getMessage());
        }
    }

    public void setExtensionFactory(ExtensionFactory extensionFactory) {
        this.extensionFactory = extensionFactory;
    }

    public void setFuture(CompletableFuture<Session> fut) {
        this.openFuture = fut;
    }

    /**
     * Set the timeout in milliseconds
     */
    @Override
    public void setIdleTimeout(long ms) {
        connection.setMaxIdleTimeout(ms);
    }

    public void setOutgoingHandler(OutgoingFrames outgoing) {
        this.outgoingHandler = outgoing;
    }

    @Deprecated
    public void setPolicy(WebSocketPolicy policy) {
        // do nothing
    }

    public void setUpgradeRequest(UpgradeRequest request) {
        this.upgradeRequest = request;
        this.protocolVersion = request.getProtocolVersion();
        this.parameterMap.clear();
        if (request.getParameterMap() != null) {
            for (Map.Entry<String, List<String>> entry : request.getParameterMap().entrySet()) {
                List<String> values = entry.getValue();
                if (values != null) {
                    this.parameterMap.put(entry.getKey(), values.toArray(new String[values.size()]));
                } else {
                    this.parameterMap.put(entry.getKey(), new String[0]);
                }
            }
        }
    }

    public void setUpgradeResponse(UpgradeResponse response) {
        this.upgradeResponse = response;
    }

    @Override
    public SuspendToken suspend() {
        return connection.suspend();
    }

    /**
     * @return the default (initial) value for the batching mode.
     */
    public BatchMode getBatchMode() {
        return BatchMode.AUTO;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebSocketSession[");
        builder.append("websocket=").append(websocket);
        builder.append(",behavior=").append(policy.getBehavior());
        builder.append(",connection=").append(connection);
        builder.append(",remote=").append(remote);
        builder.append(",incoming=").append(incomingHandler);
        builder.append(",outgoing=").append(outgoingHandler);
        builder.append("]");
        return builder.toString();
    }

    public interface Listener {
        void onOpened(WebSocketSession session);

        void onClosed(WebSocketSession session);
    }
}
