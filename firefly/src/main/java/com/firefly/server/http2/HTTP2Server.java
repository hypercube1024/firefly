package com.firefly.server.http2;

import com.firefly.codec.common.CommonDecoder;
import com.firefly.codec.common.CommonEncoder;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.ShutdownHelper;
import com.firefly.net.Server;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.ExecutorService;

public class HTTP2Server extends AbstractLifeCycle {

    private final Server server;
    private final HTTP2Configuration http2Configuration;
    private final String host;
    private final int port;

    public HTTP2Server(String host, int port, HTTP2Configuration http2Configuration,
                       ServerHTTPHandler serverHTTPHandler) {
        this(host, port, http2Configuration, new HTTP2ServerRequestHandler(serverHTTPHandler), serverHTTPHandler,
                new WebSocketHandler() {
                });
    }

    public HTTP2Server(String host, int port, HTTP2Configuration c,
                       ServerSessionListener listener,
                       ServerHTTPHandler serverHTTPHandler,
                       WebSocketHandler webSocketHandler) {
        if (c == null)
            throw new IllegalArgumentException("the http2 configuration is null");

        if (host == null)
            throw new IllegalArgumentException("the http2 server host is empty");

        this.host = host;
        this.port = port;

        c.getTcpConfiguration()
         .setDecoder(new CommonDecoder(new HTTP1ServerDecoder(new HTTP2ServerDecoder())));
        c.getTcpConfiguration()
         .setEncoder(new CommonEncoder());
        c.getTcpConfiguration()
         .setHandler(new HTTP2ServerHandler(c, listener, serverHTTPHandler, webSocketHandler));
        this.server = new AsynchronousTcpServer(c.getTcpConfiguration());
        this.http2Configuration = c;
    }

    public HTTP2Configuration getHttp2Configuration() {
        return http2Configuration;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public ExecutorService getNetExecutorService() {
        return server.getNetExecutorService();
    }

    @Override
    protected void init() {
        server.listen(host, port);
    }

    @Override
    protected void destroy() {
        if (server != null) {
            server.stop();
        }
        ShutdownHelper.destroy();
    }

}
