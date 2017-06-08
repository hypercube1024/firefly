package com.firefly.net.tcp.aio;

import com.codahale.metrics.Timer;
import com.firefly.net.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AsynchronousTcpClient extends AbstractTcpLifeCycle implements Client {

    public AsynchronousTcpClient() {
    }

    public AsynchronousTcpClient(Config config) {
        this.config = config;
    }

    public AsynchronousTcpClient(Decoder decoder, Encoder encoder, Handler handler) {
        config = new Config();
        config.setDecoder(decoder);
        config.setEncoder(encoder);
        config.setHandler(handler);
    }

    public AsynchronousTcpClient(Decoder decoder, Encoder encoder, Handler handler, int timeout) {
        config = new Config();
        config.setDecoder(decoder);
        config.setEncoder(encoder);
        config.setHandler(handler);
        config.setTimeout(timeout);
    }

    @Override
    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public int connect(String host, int port) {
        int id = sessionId.getAndIncrement();
        connect(host, port, id);
        return id;
    }

    @Override
    public void connect(String host, int port, int id) {
        start();
        try {
            Timer timer = config.getMetrics().timer("aio.AsynchronousTcpClient.connect:```" + host + ":" + port + "```");
            Timer.Context context = timer.time();
            final AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(group);
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);
            socketChannel.connect(new InetSocketAddress(host, port), id, new CompletionHandler<Void, Integer>() {

                @Override
                public void completed(Void result, Integer sessionId) {
                    worker.registerChannel(socketChannel, sessionId);
                    context.stop();
                }

                @Override
                public void failed(Throwable t, Integer sessionId) {
                    try {
                        config.getHandler().failedOpeningSession(sessionId, t);
                    } catch (Throwable e) {
                        log.error("session {} open exception", e, sessionId);
                    }
                    log.error("session {} connect error", t, sessionId);
                    context.stop();
                }
            });
        } catch (IOException e) {
            log.error("client connect error", e);
        }
    }

    @Override
    protected String getThreadName() {
        return "firefly-aio-tcp-client";
    }
}
