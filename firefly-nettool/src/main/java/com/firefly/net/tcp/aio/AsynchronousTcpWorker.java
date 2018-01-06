package com.firefly.net.tcp.aio;

import com.codahale.metrics.MetricRegistry;
import com.firefly.net.Config;
import com.firefly.net.NetEvent;
import com.firefly.net.Worker;
import com.firefly.net.tcp.aio.metric.SessionMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;

public class AsynchronousTcpWorker implements Worker {
    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final Config config;
    private final NetEvent netEvent;
    private final SessionMetric sessionMetric;

    AsynchronousTcpWorker(Config config, NetEvent netEvent) {
        this.config = config;
        this.netEvent = netEvent;
        MetricRegistry metrics = config.getMetricReporterFactory().getMetricRegistry();
        sessionMetric = new SessionMetric(metrics, "aio.tcpSession");
    }

    @Override
    public void registerChannel(Channel channel, int sessionId) {
        try {
            AsynchronousSocketChannel socketChannel = (AsynchronousSocketChannel) channel;
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);

            AsynchronousTcpSession session = new AsynchronousTcpSession(sessionId, config, sessionMetric, netEvent, socketChannel);
            netEvent.notifySessionOpened(session);
            session._read();
        } catch (IOException e) {
            log.error("socketChannel register error", e);
        }
    }

}
