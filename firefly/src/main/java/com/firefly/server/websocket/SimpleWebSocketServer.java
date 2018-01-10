package com.firefly.server.websocket;

import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.server.http2.router.handler.body.HTTPBodyConfiguration;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class SimpleWebSocketServer extends AbstractLifeCycle {

    private final HTTP2ServerBuilder serverBuilder;

    public SimpleWebSocketServer() {
        this(new SimpleHTTPServerConfiguration());
    }

    public SimpleWebSocketServer(SimpleHTTPServerConfiguration serverConfiguration) {
        this(serverConfiguration, new HTTPBodyConfiguration());
    }

    public SimpleWebSocketServer(SimpleHTTPServerConfiguration serverConfiguration,
                                 HTTPBodyConfiguration httpBodyConfiguration) {
        this.serverBuilder = new HTTP2ServerBuilder().httpServer(serverConfiguration, httpBodyConfiguration);
        start();
    }

    public HTTP2ServerBuilder.WebSocketBuilder websocket(String path) {
        return serverBuilder.websocket(path);
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {
        serverBuilder.stop();
    }
}
