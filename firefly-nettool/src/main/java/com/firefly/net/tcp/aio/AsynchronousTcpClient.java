package com.firefly.net.tcp.aio;

import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.firefly.net.*;
import com.firefly.net.event.DefaultEventManager;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AsynchronousTcpClient extends AbstractLifeCycle implements Client {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private Config config;
    private AtomicInteger sessionId = new AtomicInteger(0);
    private AsynchronousChannelGroup group;
    private AsynchronousTcpWorker worker;
    private ScheduledReporter reporter;

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
    protected void init() {
        try {
            group = AsynchronousChannelGroup.withThreadPool(new ForkJoinPool
                    (config.getAsynchronousCorePoolSize(),
                            pool -> {
                                ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                                worker.setName("firefly-aio-client-thread" + worker.getPoolIndex());
                                return worker;
                            },
                            null, true));
            log.info(config.toString());
            EventManager eventManager = new DefaultEventManager(config);
            worker = new AsynchronousTcpWorker(config, eventManager);
            if (config.isMonitorEnable()) {
                reporter = config.getReporterFactory().call(config.getMetrics());
                reporter.start(10, TimeUnit.SECONDS);
            }
        } catch (IOException e) {
            log.error("initialization client channel group error", e);
        }
    }

    @Override
    protected void destroy() {
        if (group != null) {
            group.shutdown();
        }
        if (config.isMonitorEnable()) {
            reporter.stop();
        }
        LogFactory.getInstance().stop();
        Millisecond100Clock.stop();
    }

}
