package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.model.CloseInfo;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.model.OutgoingFrames;
import com.firefly.net.Connection;
import com.firefly.utils.function.Action1;

public interface WebSocketConnection extends OutgoingFrames, SuspendToken, Connection {

    WebSocketConnection close(Action1<WebSocketConnection> closedCallback);

    WebSocketConnection exception(Action1<WebSocketConnection> exceptionCallback);

    /**
     * Called to indicate a close frame was successfully sent to the remote.
     *
     * @param close the close details
     */
    void onLocalClose(CloseInfo close);

    /**
     * Terminate the connection (no close frame sent)
     */
    void disconnect();

    /**
     * Get the read/write idle timeout.
     *
     * @return the idle timeout in milliseconds
     */
    long getIdleTimeout();

    /**
     * Get the IOState of the connection.
     *
     * @return the IOState of the connection.
     */
    IOState getIOState();

    /**
     * Set the maximum number of milliseconds of idleness before the connection is closed/disconnected, (ie no frames are either sent or received)
     *
     * @return the idle timeout in milliseconds
     */
    long getMaxIdleTimeout();

    /**
     * The policy that the connection is running under.
     *
     * @return the policy for the connection
     */
    WebSocketPolicy getPolicy();

    /**
     * Tests if the connection is actively reading.
     *
     * @return true if connection is actively attempting to read.
     */
    boolean isReading();

    /**
     * Set the maximum number of milliseconds of idleness before the connection is closed/disconnected, (ie no frames are either sent or received)
     * <p>
     * This idle timeout cannot be garunteed to take immediate effect for any active read/write actions.
     * New read/write actions will have this new idle timeout.
     *
     * @param ms the number of milliseconds of idle timeout
     */
    void setMaxIdleTimeout(long ms);

    /**
     * Set where the connection should send the incoming frames to.
     * <p>
     * Often this is from the Parser to the start of the extension stack, and eventually on to the session.
     *
     * @param incoming the incoming frames handler
     */
    void setNextIncomingFrames(IncomingFrames incoming);

    /**
     * Associate the Active Session with the connection.
     *
     * @param session the session for this connection
     */
    void setSession(WebSocketSession session);

    /**
     * Suspend a the incoming read events on the connection.
     *
     * @return the suspend token
     */
    SuspendToken suspend();

}
