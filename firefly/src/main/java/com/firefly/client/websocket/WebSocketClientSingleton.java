package com.firefly.client.websocket;

import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class WebSocketClientSingleton extends AbstractLifeCycle {

    private static WebSocketClientSingleton ourInstance = new WebSocketClientSingleton();

    public static WebSocketClientSingleton getInstance() {
        return ourInstance;
    }

    private SimpleWebSocketClient webSocketClient;

    private WebSocketClientSingleton() {
        start();
    }

    public SimpleWebSocketClient webSocketClient() {
        return webSocketClient;
    }

    @Override
    protected void init() {
        webSocketClient = new SimpleWebSocketClient();
    }

    @Override
    protected void destroy() {
        webSocketClient.stop();
    }
}
