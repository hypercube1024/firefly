package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;
import kotlinx.coroutines.channels.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 * The TCP net server.
 *
 * @author Pengtao Qiu
 */
public interface TcpServer extends LifeCycle {

    /**
     * Set the TLS engine factory.
     *
     * @param secureEngineFactory The TLS engine factory.
     * @return The TCP server.
     */
    TcpServer secureEngineFactory(SecureEngineFactory secureEngineFactory);

    /**
     * Enable the TLS protocol over the TCP connection.
     *
     * @return The TCP server.
     */
    TcpServer enableSecureConnection();

    /**
     * Accept the client TCP connection.
     *
     * @param consumer Accept the connection callback.
     * @return The TCP server.
     */
    TcpServer onAccept(Consumer<TcpConnection> consumer);

    /**
     * If you don't set a callback for the connection accepting event. The server will accept connection
     * and send it to the channel. And then, you can receive the connection from this channel.
     *
     * @return The TCP connection channel.
     */
    Channel<TcpConnection> getTcpConnectionChannel();

    /**
     * Bind a server TCP address
     *
     * @param address The server TCP address.
     * @return The TCP server.
     */
    TcpServer listen(SocketAddress address);

    /**
     * Bind the server host and port.
     *
     * @param host The server host.
     * @param port The server port.
     * @return The TCP server.
     */
    default TcpServer listen(String host, int port) {
        return listen(new InetSocketAddress(host, port));
    }

}
