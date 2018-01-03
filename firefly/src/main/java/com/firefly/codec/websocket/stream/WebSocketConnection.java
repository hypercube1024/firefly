package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.model.CloseInfo;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.model.OutgoingFrames;
import com.firefly.net.Connection;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;

public interface WebSocketConnection extends OutgoingFrames, Connection {

    WebSocketConnection onClose(Action1<WebSocketConnection> closedListener);

    WebSocketConnection onException(Action2<WebSocketConnection, Throwable> exceptionListener);

    /**
     * Called to indicate a close frame was successfully sent to the remote.
     *
     * @param close the close details
     */
    void onLocalClose(CloseInfo close);

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
     * The policy that the connection is running under.
     *
     * @return the policy for the connection
     */
    WebSocketPolicy getPolicy();

    void setIncomingFrames(IncomingFrames incomingFrames);

    IncomingFrames getIncomingFrames();

}
