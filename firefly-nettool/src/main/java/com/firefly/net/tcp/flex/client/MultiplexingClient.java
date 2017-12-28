package com.firefly.net.tcp.flex.client;

import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.codec.flex.decode.FrameParser;
import com.firefly.net.tcp.codec.flex.protocol.PingFrame;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.codec.flex.stream.impl.FlexConnectionImpl;
import com.firefly.net.tcp.codec.flex.stream.impl.FlexSession;
import com.firefly.net.tcp.flex.metric.FlexMetric;
import com.firefly.utils.CollectionUtils;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.function.Action1;
import com.firefly.utils.io.IO;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class MultiplexingClient extends AbstractLifeCycle {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");
    public static final String HEARTBEAT_KEY = "_heartbeat";

    private MultiplexingClientConfiguration configuration = new MultiplexingClientConfiguration();
    private SimpleTcpClient client;
    private Action1<FlexConnection> accept;
    private Scheduler scheduler = Schedulers.createScheduler();
    private FlexConnectionManager flexConnectionManager;
    private FlexMetric flexMetric;

    public MultiplexingClient() {
    }

    public MultiplexingClient(MultiplexingClientConfiguration configuration) {
        this.configuration = configuration;
    }

    public MultiplexingClientConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MultiplexingClientConfiguration configuration) {
        this.configuration = configuration;
    }

    public MultiplexingClient accept(Action1<FlexConnection> accept) {
        this.accept = accept;
        return this;
    }

    public CompletableFuture<FlexConnection> connect(String host, int port) {
        if (!useConnectionManager()) {
            start();
        }
        return client.connect(host, port).thenApply(connection -> {
            // create flex connection
            FlexSession session = new FlexSession(1, connection, flexMetric, configuration.getStreamMaxIdleTime(), scheduler);
            FlexConnectionImpl flexConnection = new FlexConnectionImpl(configuration, connection, session);
            connection.setAttachment(flexConnection);

            Optional.ofNullable(accept).ifPresent(a -> a.call(flexConnection));

            // set frame parser
            FrameParser frameParser = new FrameParser();
            frameParser.complete(session::notifyFrame);
            connection.receive(frameParser::receive).onException(ex -> {
                log.error("Connection " + connection.getSessionId() + " exception.", ex);
                IO.close(connection);
            });

            if (configuration.getHeartbeatInterval() > 0) {
                session.setAttribute(HEARTBEAT_KEY, scheduler.scheduleAtFixedRate(
                        () -> flexConnection.getSession().ping(new PingFrame(false)),
                        configuration.getHeartbeatInterval(),
                        configuration.getHeartbeatInterval(),
                        TimeUnit.MILLISECONDS));
                connection.onClose(session::clear)
                          .onClose(() -> Optional.ofNullable(session.getAttribute(HEARTBEAT_KEY))
                                                 .map(o -> (Scheduler.Future) o)
                                                 .ifPresent(Scheduler.Future::cancel));
            }
            return flexConnection;
        });
    }

    public FlexConnectionManager getFlexConnectionManager() {
        return flexConnectionManager;
    }

    public FlexConnection getConnection() {
        return flexConnectionManager.getConnection();
    }

    public boolean useConnectionManager() {
        return !CollectionUtils.isEmpty(configuration.getServerUrlSet());
    }

    @Override
    protected void init() {
        flexMetric = new FlexMetric(configuration.getTcpConfiguration().getMetricReporterFactory().getMetricRegistry(),
                "flex.client");
        client = new SimpleTcpClient(configuration.getTcpConfiguration());
        if (configuration.getHeartbeatInterval() <= 0) {
            configuration.setHeartbeatInterval(15 * 1000);
        }
        if (useConnectionManager()) {
            flexConnectionManager = new FlexConnectionManager(this, configuration.getServerUrlSet());
        }
    }

    @Override
    protected void destroy() {
        client.stop();
        if (configuration.getHeartbeatInterval() > 0) {
            scheduler.stop();
        }
        Optional.ofNullable(flexConnectionManager).ifPresent(FlexConnectionManager::stop);
    }
}
