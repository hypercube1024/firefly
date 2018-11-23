package com.firefly.net.tcp.aio;

import com.firefly.net.*;
import com.firefly.utils.ProjectVersion;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import static com.firefly.net.tcp.TcpPerformanceParameter.BACKLOG;

public class AsynchronousTcpServer extends AbstractTcpLifeCycle implements Server {

    public AsynchronousTcpServer() {
    }

    public AsynchronousTcpServer(Config config) {
        this.config = config;
    }

    public AsynchronousTcpServer(Decoder decoder, Encoder encoder, Handler handler) {
        config = new Config();
        config.setDecoder(decoder);
        config.setEncoder(encoder);
        config.setHandler(handler);
    }

    public AsynchronousTcpServer(Decoder decoder, Encoder encoder, Handler handler, int timeout) {
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
    public void listen(String host, int port) {
        start();
        listen(bind(host, port));
        System.out.println(ProjectVersion.getAsciiArt());
        log.info("start server. host: {}, port: {}", host, port);
    }

    private AsynchronousServerSocketChannel bind(String host, int port) {
        AsynchronousServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = AsynchronousServerSocketChannel.open(group);
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverSocketChannel.bind(new InetSocketAddress(host, port), BACKLOG);
        } catch (Exception e) {
            log.error("ServerSocket bind error", e);
        }
        return serverSocketChannel;
    }

    private void listen(final AsynchronousServerSocketChannel serverSocketChannel) {
        serverSocketChannel.accept(sessionId.getAndIncrement(), new CompletionHandler<AsynchronousSocketChannel, Integer>() {

            @Override
            public void completed(AsynchronousSocketChannel socketChannel, Integer sessionId) {
                try {
                    worker.registerChannel(socketChannel, sessionId);
                } finally {
                    listen(serverSocketChannel);
                }
            }

            @Override
            public void failed(Throwable t, Integer sessionId) {
                try {
                    try {
                        config.getHandler().failedAcceptingSession(sessionId, t);
                    } catch (Throwable e) {
                        log.error("session {} accepting exception", e, sessionId);
                    }
                    log.error("server accepts channel {} error occurs", t, sessionId);
                } finally {
                    listen(serverSocketChannel);
                }
            }
        });
    }

    @Override
    protected String getThreadName() {
        return "firefly-aio-tcp-server-";
    }
}
