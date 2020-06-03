package com.fireflysource.net.websocket.common.stream;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.websocket.common.WebSocketConnectionState;
import com.fireflysource.net.websocket.common.model.CloseInfo;
import com.fireflysource.net.websocket.common.model.StatusCode;

import java.io.EOFException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple state tracker for Input / Output and {@link ConnectionState}.
 * <p>
 * Use the various known .on*() methods to trigger a state change.
 * <ul>
 * <li>{@link #onOpen()} - connection has been opened</li>
 * </ul>
 */
public class IOState implements WebSocketConnectionState {
    /**
     * The source of a close handshake. (ie: who initiated it).
     */
    private enum CloseHandshakeSource {
        /**
         * No close handshake initiated (yet)
         */
        NONE,
        /**
         * Local side initiated the close handshake
         */
        LOCAL,
        /**
         * Remote side initiated the close handshake
         */
        REMOTE,
        /**
         * An abnormal close situation (disconnect, timeout, etc...)
         */
        ABNORMAL
    }

    public interface ConnectionStateListener {
        void onConnectionStateChange(ConnectionState state);
    }

    private static LazyLogger LOG = SystemLogger.create(IOState.class);

    private ConnectionState state;
    private final List<ConnectionStateListener> listeners = new LinkedList<>();

    /**
     * Is input on websocket available (for reading frames).
     * Used to determine close handshake completion, and track half-close states
     */
    private boolean inputAvailable;
    /**
     * Is output on websocket available (for writing frames).
     * Used to determine close handshake completion, and track half-closed states.
     */
    private boolean outputAvailable;
    /**
     * Initiator of the close handshake.
     * Used to determine who initiated a close handshake for reply reasons.
     */
    private CloseHandshakeSource closeHandshakeSource;
    /**
     * The close info for the initiator of the close handshake.
     * It is possible in abnormal close scenarios to have a different
     * final close info that is used to notify the WS-Endpoint's onClose()
     * events with.
     */
    private CloseInfo closeInfo;
    /**
     * Atomic reference to the final close info.
     * This can only be set once, and is used for the WS-Endpoint's onClose()
     * event.
     */
    private AtomicReference<CloseInfo> finalClose = new AtomicReference<>();
    /**
     * Tracker for if the close handshake was completed successfully by
     * both sides. False if close was sudden or abnormal.
     */
    private boolean cleanClose;

    /**
     * Create a new IOState, initialized to {@link ConnectionState#CONNECTING}
     */
    public IOState() {
        this.state = ConnectionState.CONNECTING;
        this.inputAvailable = false;
        this.outputAvailable = false;
        this.closeHandshakeSource = CloseHandshakeSource.NONE;
        this.closeInfo = null;
        this.cleanClose = false;
    }

    public void addListener(ConnectionStateListener listener) {
        listeners.add(listener);
    }

    public CloseInfo getCloseInfo() {
        CloseInfo ci = finalClose.get();
        if (ci != null) {
            return ci;
        }
        return closeInfo;
    }

    public ConnectionState getConnectionState() {
        return state;
    }

    public boolean isClosed() {
        return (state == ConnectionState.CLOSED);
    }

    public boolean isInputAvailable() {
        return inputAvailable;
    }

    public boolean isOpen() {
        return !isClosed();
    }

    public boolean isOutputAvailable() {
        return outputAvailable;
    }

    private void notifyStateListeners(ConnectionState state) {
        if (LOG.isDebugEnabled())
            LOG.debug("Notify State Listeners: {}", state);
        for (ConnectionStateListener listener : listeners) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("{}.onConnectionStateChange({})", listener.getClass().getSimpleName(), state.name());
            }
            try {
                listener.onConnectionStateChange(state);
            } catch (Exception e) {
                LOG.error("handle websocket connection state change event exception.", e);
            }
        }
    }

    /**
     * A websocket connection has been disconnected for abnormal close reasons.
     * <p>
     * This is the low level disconnect of the socket. It could be the result of a normal close operation, from an IO error, or even from a timeout.
     *
     * @param close the close information
     */
    public void onAbnormalClose(CloseInfo close) {
        if (LOG.isDebugEnabled())
            LOG.debug("onAbnormalClose({})", close);

        if (this.state == ConnectionState.CLOSED) {
            // already closed
            return;
        }

        if (this.state == ConnectionState.OPEN) {
            this.cleanClose = false;
        }

        this.state = ConnectionState.CLOSED;
        finalClose.compareAndSet(null, close);
        this.inputAvailable = false;
        this.outputAvailable = false;
        this.closeHandshakeSource = CloseHandshakeSource.ABNORMAL;
        ConnectionState event = this.state;
        notifyStateListeners(event);
    }

    /**
     * A close handshake has been issued from the local endpoint
     *
     * @param closeInfo the close information
     */
    public void onCloseLocal(CloseInfo closeInfo) {
        boolean open = false;

        ConnectionState initialState = this.state;
        if (LOG.isDebugEnabled())
            LOG.debug("onCloseLocal({}) : {}", closeInfo, initialState);
        if (initialState == ConnectionState.CLOSED) {
            // already closed
            if (LOG.isDebugEnabled())
                LOG.debug("already closed");
            return;
        }

        if (initialState == ConnectionState.CONNECTED) {
            // fast close. a local close request from end-user onConnect/onOpen method
            if (LOG.isDebugEnabled())
                LOG.debug("FastClose in CONNECTED detected");
            open = true;
        }

        if (open)
            openAndCloseLocal(closeInfo);
        else
            closeLocal(closeInfo);
    }

    private void openAndCloseLocal(CloseInfo closeInfo) {
        // Force the state open (to allow read/write to endpoint)
        onOpen();
        if (LOG.isDebugEnabled())
            LOG.debug("FastClose continuing with Closure");
        closeLocal(closeInfo);
    }

    private void closeLocal(CloseInfo closeInfo) {
        ConnectionState event = null;
        ConnectionState abnormalEvent = null;

        if (LOG.isDebugEnabled())
            LOG.debug("onCloseLocal(), input={}, output={}", inputAvailable, outputAvailable);

        this.closeInfo = closeInfo;

        // Turn off further output.
        outputAvailable = false;

        if (closeHandshakeSource == CloseHandshakeSource.NONE) {
            closeHandshakeSource = CloseHandshakeSource.LOCAL;
        }

        if (!inputAvailable) {
            if (LOG.isDebugEnabled())
                LOG.debug("Close Handshake satisfied, disconnecting");
            cleanClose = true;
            this.state = ConnectionState.CLOSED;
            finalClose.compareAndSet(null, closeInfo);
            event = this.state;
        } else if (this.state == ConnectionState.OPEN) {
            // We are now entering CLOSING (or half-closed).
            this.state = ConnectionState.CLOSING;
            event = this.state;

            // If abnormal, we don't expect an answer.
            if (closeInfo.isAbnormal()) {
                abnormalEvent = ConnectionState.CLOSED;
                finalClose.compareAndSet(null, closeInfo);
                cleanClose = false;
                outputAvailable = false;
                inputAvailable = false;
                closeHandshakeSource = CloseHandshakeSource.ABNORMAL;
            }
        }

        // Only notify on state change events
        if (event != null) {
            notifyStateListeners(event);
            if (abnormalEvent != null) {
                notifyStateListeners(abnormalEvent);
            }
        }
    }

    /**
     * A close handshake has been received from the remote endpoint
     *
     * @param closeInfo the close information
     */
    public void onCloseRemote(CloseInfo closeInfo) {
        if (LOG.isDebugEnabled())
            LOG.debug("onCloseRemote({})", closeInfo);

        if (this.state == ConnectionState.CLOSED) {
            // already closed
            return;
        }

        if (LOG.isDebugEnabled())
            LOG.debug("onCloseRemote(), input={}, output={}", inputAvailable, outputAvailable);

        this.closeInfo = closeInfo;

        // turn off further input
        inputAvailable = false;

        if (closeHandshakeSource == CloseHandshakeSource.NONE) {
            closeHandshakeSource = CloseHandshakeSource.REMOTE;
        }

        ConnectionState event = null;
        if (!outputAvailable) {
            LOG.debug("Close Handshake satisfied, disconnecting");
            cleanClose = true;
            state = ConnectionState.CLOSED;
            finalClose.compareAndSet(null, closeInfo);
            event = this.state;
        } else if (this.state == ConnectionState.OPEN) {
            // We are now entering CLOSING (or half-closed)
            this.state = ConnectionState.CLOSING;
            event = this.state;
        }


        // Only notify on state change events
        if (event != null) {
            notifyStateListeners(event);
        }
    }

    /**
     * WebSocket has successfully upgraded, but the end-user onOpen call hasn't run yet.
     * <p>
     * This is an intermediate state between the RFC's {@link ConnectionState#CONNECTING} and {@link ConnectionState#OPEN}
     */
    public void onConnected() {
        if (this.state != ConnectionState.CONNECTING) {
            LOG.debug("Unable to set to connected, not in CONNECTING state: {}", this.state);
            return;
        }

        this.state = ConnectionState.CONNECTED;
        inputAvailable = false; // cannot read (yet)
        outputAvailable = true; // write allowed
        ConnectionState event = this.state;
        notifyStateListeners(event);
    }

    /**
     * A websocket connection has finished its upgrade handshake, and is now open.
     */
    public void onOpen() {
        if (LOG.isDebugEnabled())
            LOG.debug("onOpened()");

        if (this.state == ConnectionState.OPEN) {
            // already opened
            return;
        }

        if (this.state != ConnectionState.CONNECTED) {
            LOG.debug("Unable to open, not in CONNECTED state: {}", this.state);
            return;
        }

        this.state = ConnectionState.OPEN;
        this.inputAvailable = true;
        this.outputAvailable = true;
        ConnectionState event = this.state;

        notifyStateListeners(event);
    }

    /**
     * The local endpoint has reached a read failure.
     * <p>
     * This could be a normal result after a proper close handshake, or even a premature close due to a connection disconnect.
     *
     * @param t the read failure
     */
    public void onReadFailure(Throwable t) {
        if (this.state == ConnectionState.CLOSED) {
            // already closed
            return;
        }

        // Build out Close Reason
        String reason = "WebSocket Read Failure";
        if (t instanceof EOFException) {
            reason = "WebSocket Read EOF";
            Throwable cause = t.getCause();
            if ((cause != null) && (StringUtils.hasText(cause.getMessage()))) {
                reason = "EOF: " + cause.getMessage();
            }
        } else {
            if (StringUtils.hasText(t.getMessage())) {
                reason = t.getMessage();
            }
        }

        CloseInfo close = new CloseInfo(StatusCode.ABNORMAL, reason);
        finalClose.compareAndSet(null, close);
        closeAndNotify(close);
    }

    /**
     * The local endpoint has reached a write failure.
     * <p>
     * A low level I/O failure, or even a firefly side EndPoint close (from idle timeout) are a few scenarios
     *
     * @param t the throwable that caused the write failure
     */
    public void onWriteFailure(Throwable t) {
        if (this.state == ConnectionState.CLOSED) {
            // already closed
            return;
        }

        // Build out Close Reason
        String reason = "WebSocket Write Failure";
        if (t instanceof EOFException) {
            reason = "WebSocket Write EOF";
            Throwable cause = t.getCause();
            if ((cause != null) && (StringUtils.hasText(cause.getMessage()))) {
                reason = "EOF: " + cause.getMessage();
            }
        } else {
            if (StringUtils.hasText(t.getMessage())) {
                reason = t.getMessage();
            }
        }

        CloseInfo close = new CloseInfo(StatusCode.ABNORMAL, reason);

        finalClose.compareAndSet(null, close);

        this.cleanClose = false;
        this.state = ConnectionState.CLOSED;
        this.inputAvailable = false;
        this.outputAvailable = false;
        this.closeHandshakeSource = CloseHandshakeSource.ABNORMAL;
        ConnectionState event = this.state;

        notifyStateListeners(event);
    }

    public void onDisconnected() {
        if (this.state == ConnectionState.CLOSED) {
            // already closed
            return;
        }

        CloseInfo close = new CloseInfo(StatusCode.ABNORMAL, "Disconnected");
        closeAndNotify(close);
    }

    private void closeAndNotify(CloseInfo close) {
        this.cleanClose = false;
        this.state = ConnectionState.CLOSED;
        this.closeInfo = close;
        this.inputAvailable = false;
        this.outputAvailable = false;
        this.closeHandshakeSource = CloseHandshakeSource.ABNORMAL;
        ConnectionState event = this.state;
        notifyStateListeners(event);
    }

    public boolean isAbnormalClose() {
        return closeHandshakeSource == CloseHandshakeSource.ABNORMAL;
    }

    public boolean isCleanClose() {
        return cleanClose;
    }

    public boolean isLocalCloseInitiated() {
        return closeHandshakeSource == CloseHandshakeSource.LOCAL;
    }

    public boolean isRemoteCloseInitiated() {
        return closeHandshakeSource == CloseHandshakeSource.REMOTE;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(this.getClass().getSimpleName());
        str.append("@").append(Integer.toHexString(hashCode()));
        str.append("[").append(state);
        str.append(',');
        if (!inputAvailable) {
            str.append('!');
        }
        str.append("in,");
        if (!outputAvailable) {
            str.append('!');
        }
        str.append("out");
        if ((state == ConnectionState.CLOSED) || (state == ConnectionState.CLOSING)) {
            CloseInfo ci = finalClose.get();
            if (ci != null) {
                str.append(",finalClose=").append(ci);
            } else {
                str.append(",close=").append(closeInfo);
            }
            str.append(",clean=").append(cleanClose);
            str.append(",closeSource=").append(closeHandshakeSource);
        }
        str.append(']');
        return str.toString();
    }
}
