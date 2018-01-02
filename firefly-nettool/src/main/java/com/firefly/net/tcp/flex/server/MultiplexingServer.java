package com.firefly.net.tcp.flex.server;

import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.codec.flex.decode.FrameParser;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.codec.flex.stream.impl.FlexConnectionImpl;
import com.firefly.net.tcp.codec.flex.stream.impl.FlexSession;
import com.firefly.net.tcp.flex.metric.FlexMetric;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.function.Action1;
import com.firefly.utils.io.IO;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengtao Qiu
 */
public class MultiplexingServer extends AbstractLifeCycle {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    private MultiplexingServerConfiguration configuration = new MultiplexingServerConfiguration();
    private SimpleTcpServer server;
    private Action1<FlexConnection> accept;
    private Scheduler scheduler = Schedulers.createScheduler();

    public MultiplexingServer() {
    }

    public MultiplexingServer(MultiplexingServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public MultiplexingServerConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MultiplexingServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public void listen(String host, int port) {
        configuration.getTcpServerConfiguration().setHost(host);
        configuration.getTcpServerConfiguration().setPort(port);
        start();
    }

    public void listen() {
        start();
    }

    public MultiplexingServer accept(Action1<FlexConnection> accept) {
        this.accept = accept;
        return this;
    }

    @Override
    protected void init() {
        FlexMetric flexMetric = new FlexMetric(configuration.getTcpServerConfiguration().getMetricReporterFactory().getMetricRegistry(),
                "flex.server");
        server = new SimpleTcpServer(configuration.getTcpServerConfiguration());
        server.accept(connection -> {
            // create flex connection
            FlexSession session = new FlexSession(2, connection, flexMetric, configuration.getStreamMaxIdleTime(), scheduler);
            FlexConnectionImpl flexConnection = new FlexConnectionImpl(configuration, connection, session);
            connection.setAttachment(flexConnection);
            accept.call(flexConnection);

            // set frame parser
            FrameParser frameParser = new FrameParser();
            frameParser.complete(session::notifyFrame);
            connection.receive(frameParser::receive).onException(ex -> {
                log.error("Connection " + connection.getSessionId() + " exception.", ex);
                IO.close(connection);
            }).onClose(session::clear);

        });
        server.listen(configuration.getTcpServerConfiguration().getHost(), configuration.getTcpServerConfiguration().getPort());
    }

    @Override
    protected void destroy() {
        scheduler.stop();
        server.stop();
    }
}
