package com.firefly.client.websocket;

import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.Collections;

/**
 * @author Pengtao Qiu
 */
public class SecureWebSocketClientSingleton extends AbstractLifeCycle {
    private static SecureWebSocketClientSingleton ourInstance = new SecureWebSocketClientSingleton();

    public static SecureWebSocketClientSingleton getInstance() {
        return ourInstance;
    }

    private SimpleWebSocketClient webSocketClient;

    private SecureWebSocketClientSingleton() {
        start();
    }

    public SimpleWebSocketClient secureWebSocketClient() {
        return webSocketClient;
    }

    @Override
    protected void init() {
        SimpleHTTPClientConfiguration http2Configuration = new SimpleHTTPClientConfiguration();
        http2Configuration.setSecureConnectionEnabled(true);
        http2Configuration.getSecureSessionFactory().setSupportedProtocols(Collections.singletonList("http/1.1"));
        webSocketClient = new SimpleWebSocketClient(http2Configuration);
    }

    @Override
    protected void destroy() {
        webSocketClient.stop();
    }
}
