package com.fireflysource.net.websocket.server;

import com.fireflysource.net.http.server.HttpServer;
import com.fireflysource.net.websocket.common.WebSocketMessageHandler;
import com.fireflysource.net.websocket.common.model.WebSocketPolicy;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketServerConnectionBuilder {

    /**
     * Set the websocket url.
     *
     * @param url The websocket url.
     * @return The websocket server connection builder.
     */
    WebSocketServerConnectionBuilder url(String url);

    /**
     * Select the extensions.
     *
     * @param selector The websocket extension selector.
     * @return The websocket server connection builder.
     */
    WebSocketServerConnectionBuilder onExtensionSelect(ExtensionSelector selector);

    /**
     * Select the sub protocols.
     *
     * @param selector The websocket sub protocol selector.
     * @return The websocket server connection builder.
     */
    WebSocketServerConnectionBuilder onSubProtocolSelect(SubProtocolSelector selector);

    /**
     * Set the websocket policy.
     *
     * @param policy The websocket policy.
     * @return The websocket server connection builder.
     */
    WebSocketServerConnectionBuilder policy(WebSocketPolicy policy);

    /**
     * Set the websocket message handler.
     *
     * @param handler The websocket message handler.
     * @return The websocket server connection builder.
     */
    WebSocketServerConnectionBuilder onMessage(WebSocketMessageHandler handler);

    /**
     * Set the websocket connection listener.
     *
     * @param listener The websocket connection listener.
     * @return The HTTP server.
     */
    HttpServer onAccept(WebSocketServerConnectionListener listener);

}
