package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;
import kotlinx.coroutines.channels.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
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
     * The supported application layer protocols.
     *
     * @param supportedProtocols The supported application layer protocols.
     * @return The TCP server.
     */
    TcpServer supportedProtocols(List<String> supportedProtocols);

    /**
     * Create a TLS engine using advisory peer information.
     * Applications using this factory method are providing hints for an internal session reuse strategy.
     * Some cipher suites (such as Kerberos) require remote hostname information, in which case peerHost needs to be specified.
     *
     * @param peerHost the non-authoritative name of the host.
     * @return The TCP server.
     */
    TcpServer peerHost(String peerHost);

    /**
     * Create a TLS engine using advisory peer information.
     * Applications using this factory method are providing hints for an internal session reuse strategy.
     * Some cipher suites (such as Kerberos) require remote hostname information, in which case peerHost needs to be specified.
     *
     * @param peerPort the non-authoritative port.
     * @return The TCP server.
     */
    TcpServer peerPort(int peerPort);

    /**
     * Enable the TLS protocol over the TCP connection.
     *
     * @return The TCP server.
     */
    TcpServer enableSecureConnection();

    /**
     * Set the TCP idle timeout. The unit is second.
     *
     * @param timeout The TCP idle timeout. Time unit is second.
     * @return The TCP server.
     */
    TcpServer timeout(Long timeout);

    /**
     * Accept the client TCP connection.
     *
     * @param consumer Accept the connection callback.
     * @return The TCP server.
     */
    TcpServer onAccept(Consumer<TcpConnection> consumer);

    /**
     * If you don't set a callback for the connection accepting event. The server will accept connection
     * and send it to the channel, and then, you can receive the connection from this channel.
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
