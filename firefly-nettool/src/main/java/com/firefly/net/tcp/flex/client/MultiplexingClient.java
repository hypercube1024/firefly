package com.firefly.net.tcp.flex.client;

import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.codec.flex.decode.FrameParser;
import com.firefly.net.tcp.codec.flex.protocol.PingFrame;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.codec.flex.stream.impl.FlexConnectionImpl;
import com.firefly.net.tcp.codec.flex.stream.impl.FlexSession;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
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
    private Scheduler heartbeatScheduler = Schedulers.createScheduler();

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

    public CompletableFuture<FlexConnection> connect(String host, int port) {
        start();
        return client.connect(host, port).thenApply(connection -> {
            // create flex connection
            FlexSession session = new FlexSession(1, connection);
            FlexConnectionImpl flexConnection = new FlexConnectionImpl(configuration, connection, session);
            connection.setAttachment(flexConnection);

            // set frame parser
            FrameParser frameParser = new FrameParser();
            frameParser.complete(session::notifyFrame);
            connection.receive(frameParser::receive).exception(ex -> {
                log.error("Connection " + connection.getSessionId() + " exception.", ex);
                IO.close(connection);
            });

            if (configuration.getHeartbeatInterval() > 0) {
                session.setAttribute(HEARTBEAT_KEY, heartbeatScheduler.scheduleAtFixedRate(
                        () -> flexConnection.getSession().ping(new PingFrame(false)),
                        configuration.getHeartbeatInterval(),
                        configuration.getHeartbeatInterval(),
                        TimeUnit.MILLISECONDS));
                connection.close(() -> Optional.ofNullable(session.getAttribute(HEARTBEAT_KEY))
                                               .map(o -> (Scheduler.Future) o)
                                               .ifPresent(Scheduler.Future::cancel));
            }
            return flexConnection;
        });
    }

    @Override
    protected void init() {
        client = new SimpleTcpClient(configuration.getTcpConfiguration());
        if (configuration.getHeartbeatInterval() <= 0) {
            configuration.setHeartbeatInterval(15 * 1000);
        }
    }

    @Override
    protected void destroy() {
        client.stop();
        if (configuration.getHeartbeatInterval() > 0) {
            heartbeatScheduler.stop();
        }
    }
}
