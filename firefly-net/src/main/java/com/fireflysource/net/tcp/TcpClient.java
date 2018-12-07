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
     * Create a TCP connection to the server.
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
     * Create a TLS connection to the server.
     *
     * @param address            The server address.
     * @param supportedProtocols The supported application layer protocols.
     * @return The TCP connection.
     */
    CompletableFuture<TcpConnection> connect(SocketAddress address, List<String> supportedProtocols);

    /**
     * Create a TLS connection to the server.
     *
     * @param address            The server address.
     * @param peerHost           the non-authoritative name of the host.
     * @param peerPort           the non-authoritative port.
     * @param supportedProtocols The supported application layer protocols.
     * @return The TCP connection.
     */
    CompletableFuture<TcpConnection> connect(SocketAddress address, String peerHost, int peerPort, List<String> supportedProtocols);


}
