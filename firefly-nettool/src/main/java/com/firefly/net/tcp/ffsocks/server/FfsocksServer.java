package com.firefly.net.tcp.ffsocks.server;

import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.codec.ffsocks.decode.FrameParser;
import com.firefly.net.tcp.codec.ffsocks.stream.FfsocksConnection;
import com.firefly.net.tcp.codec.ffsocks.stream.impl.FfsocksConnectionImpl;
import com.firefly.net.tcp.codec.ffsocks.stream.impl.FfsocksSession;
import com.firefly.utils.function.Action1;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class FfsocksServer extends AbstractLifeCycle {

    private ServerFfsocksConfiguration configuration = new ServerFfsocksConfiguration();
    private SimpleTcpServer server;
    private Action1<FfsocksConnection> accept;

    public FfsocksServer() {
    }

    public FfsocksServer(ServerFfsocksConfiguration configuration) {
        this.configuration = configuration;
    }

    public ServerFfsocksConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ServerFfsocksConfiguration configuration) {
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

    public FfsocksServer accept(Action1<FfsocksConnection> accept) {
        this.accept = accept;
        return this;
    }

    @Override
    protected void init() {
        server = new SimpleTcpServer(configuration.getTcpServerConfiguration());
        server.accept(connection -> {
            FfsocksSession session = new FfsocksSession(2, connection);
            FfsocksConnectionImpl ffsocksConnection = new FfsocksConnectionImpl(configuration, connection, session);
            connection.setAttachment(ffsocksConnection);
            accept.call(ffsocksConnection);
            FrameParser frameParser = new FrameParser();
            frameParser.complete(session::notifyFrame);
            connection.receive(frameParser::receive);
        });
        server.listen(configuration.getTcpServerConfiguration().getHost(), configuration.getTcpServerConfiguration().getPort());
    }

    @Override
    protected void destroy() {
        server.stop();
    }
}
