package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The TCP net client.
 *
 * @author Pengtao Qiu
 */
public interface TcpClient extends LifeCycle {

    /**
     * Set the TCP channel group.
     *
     * @param group The TCP channel group.
     * @return The TCP client.
     */
    TcpClient tcpChannelGroup(TcpChannelGroup group);

    /**
     * Stop the TCP group when the TCP client stops.
     *
     * @param stop If true, stop the TCP group when the TCP client stops.
     * @return The TCP client.
     */
    TcpClient stopTcpChannelGroup(boolean stop);

    /**
     * Set the TLS engine factory.
     *
     * @param secureEngineFactory The TLS engine factory.
     * @return The TCP client.
     */
    TcpClient secureEngineFactory(SecureEngineFactory secureEngineFactory);

    /**
     * Enable the TLS protocol over the TCP connection.
     *
     * @return The TCP client.
     */
    TcpClient enableSecureConnection();

    /**
     * Set the TCP idle timeout. The unit is second.
     *
     * @param timeout The TCP idle timeout. The unit is second.
     * @return The TCP client.
     */
    TcpClient timeout(Long timeout);

    /**
     * Enable output buffer.
     *
     * @return The TCP client.
     */
    TcpClient enableOutputBuffer();

    /**
     * Set output buffer size.
     *
     * @param bufferSize The output buffer size.
     * @return The TCP client.
     */
    TcpClient bufferSize(int bufferSize);

    /**
     * Create a TCP connection.
     *
     * @param address The server address.
     * @return The TCP connection.
     */
    CompletableFuture<TcpConnection> connect(SocketAddress address);

    /**
     * Create a TCP connection to the server.
     *
     * @param host The server host.
     * @param port The server port.
     * @return The TCP connection.
     */
    default CompletableFuture<TcpConnection> connect(String host, int port) {
        return connect(new InetSocketAddress(host, port));
    }

    /**
     * If you enable TLS connection, tt creates a TLS connection and set the supported application layer protocols.
     *
     * @param address            The server address.
     * @param supportedProtocols The supported application layer protocols.
     * @return The TCP connection.
     */
    CompletableFuture<TcpConnection> connect(SocketAddress address, List<String> supportedProtocols);

    /**
     * If you enable TLS connection, tt creates a TLS connection using advisory peer information.
     * Applications using this factory method are providing hints for an internal session reuse strategy.
     * Some cipher suites (such as Kerberos) require remote hostname information, in which case peerHost needs to be specified.
     *
     * @param address            The server address.
     * @param peerHost           the non-authoritative name of the host.
     * @param peerPort           the non-authoritative port.
     * @param supportedProtocols The supported application layer protocols.
     * @return The TCP connection.
     */
    CompletableFuture<TcpConnection> connect(SocketAddress address, String peerHost, int peerPort, List<String> supportedProtocols);


}
