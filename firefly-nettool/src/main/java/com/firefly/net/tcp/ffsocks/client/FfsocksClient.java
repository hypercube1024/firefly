package com.firefly.net.tcp.ffsocks.client;

import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.codec.ffsocks.decode.FrameParser;
import com.firefly.net.tcp.codec.ffsocks.protocol.PingFrame;
import com.firefly.net.tcp.codec.ffsocks.stream.FfsocksConnection;
import com.firefly.net.tcp.codec.ffsocks.stream.impl.FfsocksConnectionImpl;
import com.firefly.net.tcp.codec.ffsocks.stream.impl.FfsocksSession;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class FfsocksClient extends AbstractLifeCycle {

    public static final String HEARTBEAT_KEY = "_heartbeat";

    private ClientFfsocksConfiguration configuration = new ClientFfsocksConfiguration();
    private SimpleTcpClient client;
    private Scheduler heartbeatScheduler = Schedulers.createScheduler();

    public FfsocksClient() {
    }

    public FfsocksClient(ClientFfsocksConfiguration configuration) {
        this.configuration = configuration;
    }

    public ClientFfsocksConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ClientFfsocksConfiguration configuration) {
        this.configuration = configuration;
    }

    public CompletableFuture<FfsocksConnection> connect(String host, int port) {
        start();
        return client.connect(host, port).thenApply(connection -> {
            FfsocksSession session = new FfsocksSession(1, connection);
            FfsocksConnectionImpl ffsocksConnection = new FfsocksConnectionImpl(configuration, connection, session);
            connection.setAttachment(ffsocksConnection);
            FrameParser frameParser = new FrameParser();
            frameParser.complete(session::notifyFrame);
            connection.receive(frameParser::receive);

            if (configuration.getHeartbeatInterval() > 0) {
                session.setAttribute(HEARTBEAT_KEY, heartbeatScheduler.scheduleAtFixedRate(
                        () -> ffsocksConnection.getSession().ping(new PingFrame(false)),
                        configuration.getHeartbeatInterval(),
                        configuration.getHeartbeatInterval(),
                        TimeUnit.MILLISECONDS));
                connection.close(() -> Optional.ofNullable(session.getAttribute(HEARTBEAT_KEY))
                                               .map(o -> (Scheduler.Future) o)
                                               .ifPresent(Scheduler.Future::cancel));
            }
            return ffsocksConnection;
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
