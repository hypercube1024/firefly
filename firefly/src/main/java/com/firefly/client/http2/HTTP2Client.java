package com.firefly.client.http2;

import com.firefly.codec.common.CommonDecoder;
import com.firefly.codec.common.CommonEncoder;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.codec.http2.stream.ShutdownHelper;
import com.firefly.net.Client;
import com.firefly.net.DecoderChain;
import com.firefly.net.EncoderChain;
import com.firefly.net.tcp.aio.AsynchronousTcpClient;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HTTP2Client extends AbstractLifeCycle {

    private final Client client;
    private final Map<Integer, HTTP2ClientContext> http2ClientContext = new ConcurrentHashMap<>();
    private final AtomicInteger sessionId = new AtomicInteger(0);
    private final HTTP2Configuration http2Configuration;

    public HTTP2Client(HTTP2Configuration c) {
        if (c == null)
            throw new IllegalArgumentException("the http2 configuration is null");

        c.getTcpConfiguration()
         .setDecoder(new CommonDecoder(new HTTP1ClientDecoder(new HTTP2ClientDecoder())));
        c.getTcpConfiguration()
         .setEncoder(new CommonEncoder());
        c.getTcpConfiguration()
         .setHandler(new HTTP2ClientHandler(c, http2ClientContext));

        this.client = new AsynchronousTcpClient(c.getTcpConfiguration());
        this.http2Configuration = c;
    }

    public Promise.Completable<HTTPClientConnection> connect(String host, int port) {
        Promise.Completable<HTTPClientConnection> completable = new Promise.Completable<>();
        connect(host, port, completable);
        return completable;
    }

    public void connect(String host, int port, Promise<HTTPClientConnection> promise) {
        connect(host, port, promise, new ClientHTTP2SessionListener());
    }

    public void connect(String host, int port, Promise<HTTPClientConnection> promise, ClientHTTP2SessionListener listener) {
        start();
        HTTP2ClientContext context = new HTTP2ClientContext();
        context.setPromise(promise);
        context.setListener(listener);
        int id = sessionId.getAndIncrement();
        http2ClientContext.put(id, context);
        client.connect(host, port, id);
    }

    public HTTP2Configuration getHttp2Configuration() {
        return http2Configuration;
    }

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
        if (client != null) {
            client.stop();
        }
        ShutdownHelper.destroy();
    }

}
