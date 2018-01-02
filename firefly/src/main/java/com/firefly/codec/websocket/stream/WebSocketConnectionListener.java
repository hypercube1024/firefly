package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.model.StatusCode;

/**
 * Core WebSocket Connection Listener
 */
public interface WebSocketConnectionListener {
    /**
     * A Close Event was received.
     * <p>
     * The underlying Connection will be considered closed at this point.
     *
     * @param statusCode the close status code. (See {@link StatusCode})
     * @param reason     the optional reason for the close.
     */
    void onWebSocketClose(int statusCode, String reason);

    /**
     * A WebSocket {@link Session} has connected successfully and is ready to be used.
     * <p>
     * Note: It is a good idea to track this session as a field in your object so that you can write messages back via the {@link RemoteEndpoint}
     *
     * @param session the websocket session.
     */
    void onWebSocketConnect(Session session);

    /**
     * A WebSocket exception has occurred.
     * <p>
     * This is a way for the internal implementation to notify of exceptions occured during the processing of websocket.
     * <p>
     * Usually this occurs from bad / malformed incoming packets. (example: bad UTF8 data, frames that are too big, violations of the spec)
     * <p>
     * This will result in the {@link Session} being closed by the implementing side.
     *
     * @param cause the error that occurred.
     */
    void onWebSocketError(Throwable cause);
}
