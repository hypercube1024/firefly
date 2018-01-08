package com.firefly.server.http2;

import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketHandler {

    Logger log = LoggerFactory.getLogger("firefly-system");

    WebSocketPolicy defaultWebSocketPolicy = WebSocketPolicy.newServerPolicy();

    default boolean acceptUpgrade(MetaData.Request request, MetaData.Response response,
                                  HTTPOutputStream output,
                                  HTTPConnection connection) {
        log.info("The connection {} will upgrade to WebSocket connection", connection.getSessionId());
        return true;
    }

    default void onConnect(WebSocketConnection webSocketConnection) {

    }

    default WebSocketPolicy getWebSocketPolicy() {
        return defaultWebSocketPolicy;
    }

    default void onFrame(Frame frame, WebSocketConnection connection) {
        if (log.isDebugEnabled()) {
            log.debug("The WebSocket connection {} received a  frame: {}", connection.getSessionId(), frame.toString());
        }
    }

    default void onError(Throwable t, WebSocketConnection connection) {
        log.error("The WebSocket error", t);
    }

}
