package com.firefly.net.tcp.ffsocks.client;

import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.codec.ffsocks.decode.FrameParser;
import com.firefly.net.tcp.codec.ffsocks.stream.FfsocksConnection;
import com.firefly.net.tcp.codec.ffsocks.stream.impl.FfsocksConnectionImpl;
import com.firefly.net.tcp.codec.ffsocks.stream.impl.FfsocksSession;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class FfsocksClient extends AbstractLifeCycle {

    private ClientFfsocksConfiguration configuration = new ClientFfsocksConfiguration();
    private SimpleTcpClient client;

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
            return ffsocksConnection;
        });
    }

    @Override
    protected void init() {
        client = new SimpleTcpClient(configuration.getTcpConfiguration());
    }

    @Override
    protected void destroy() {
        client.stop();
    }
}
