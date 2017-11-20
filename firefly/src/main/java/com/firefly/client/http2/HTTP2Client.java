package com.firefly.client.http2;

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

    public HTTP2Client(HTTP2Configuration http2Configuration) {
        if (http2Configuration == null)
            throw new IllegalArgumentException("the http2 configuration is null");

        DecoderChain decoder;
        EncoderChain encoder;
        if (http2Configuration.isSecureConnectionEnabled()) {
            decoder = new ClientSecureDecoder(new HTTP1ClientDecoder(new HTTP2ClientDecoder()));
            encoder = new HTTP1ClientEncoder(new HTTP2ClientEncoder(new ClientSecureEncoder()));
        } else {
            decoder = new HTTP1ClientDecoder(new HTTP2ClientDecoder());
            encoder = new HTTP1ClientEncoder(new HTTP2ClientEncoder());
        }

        http2Configuration.getTcpConfiguration().setDecoder(decoder);
        http2Configuration.getTcpConfiguration().setEncoder(encoder);
        http2Configuration.getTcpConfiguration().setHandler(new HTTP2ClientHandler(http2Configuration, http2ClientContext));

        this.client = new AsynchronousTcpClient(http2Configuration.getTcpConfiguration());
        this.http2Configuration = http2Configuration;
    }

    public Promise.Completable<HTTPClientConnection> connect(String host, int port) {
        Promise.Completable<HTTPClientConnection> completable = new Promise.Completable<>();
        connect(host, port, completable);
        return completable;
    }

    public void connect(String host, int port, Promise<HTTPClientConnection> promise) {
        connect(host, port, promise, new Listener.Adapter());
    }

    public void connect(String host, int port, Promise<HTTPClientConnection> promise, Listener listener) {
        start();
        HTTP2ClientContext context = new HTTP2ClientContext();
        context.promise = promise;
        context.listener = listener;
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
