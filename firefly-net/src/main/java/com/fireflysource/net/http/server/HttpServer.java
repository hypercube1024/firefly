package com.fireflysource.net.http.server;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface HttpServer extends LifeCycle {

    /**
     * Register a new router.
     *
     * @return The router.
     */
    Router router();

    /**
     * Register a new router.
     *
     * @param id The router id.
     * @return The router.
     */
    Router router(int id);

    /**
     * Set the TLS engine factory.
     *
     * @param secureEngineFactory The TLS engine factory.
     * @return The HTTP server.
     */
    HttpServer secureEngineFactory(SecureEngineFactory secureEngineFactory);

    /**
     * The supported application layer protocols.
     *
     * @param supportedProtocols The supported application layer protocols.
     * @return The HTTP server.
     */
    HttpServer supportedProtocols(List<String> supportedProtocols);

    /**
     * Create a TLS engine using advisory peer information.
     * Applications using this factory method are providing hints for an internal session reuse strategy.
     * Some cipher suites (such as Kerberos) require remote hostname information, in which case peerHost needs to be specified.
     *
     * @param peerHost the non-authoritative name of the host.
     * @return The HTTP server.
     */
    HttpServer peerHost(String peerHost);

    /**
     * Create a TLS engine using advisory peer information.
     * Applications using this factory method are providing hints for an internal session reuse strategy.
     * Some cipher suites (such as Kerberos) require remote hostname information, in which case peerHost needs to be specified.
     *
     * @param peerPort the non-authoritative port.
     * @return The HTTP server.
     */
    HttpServer peerPort(int peerPort);

    /**
     * Enable the TLS protocol over the TCP connection.
     *
     * @return The HTTP server.
     */
    HttpServer enableSecureConnection();

    /**
     * Set the TCP idle timeout. The unit is second.
     *
     * @param timeout The TCP idle timeout. Time unit is second.
     * @return The HTTP server.
     */
    HttpServer timeout(Long timeout);

    /**
     * Bind a server TCP address
     *
     * @param address The server TCP address.
     */
    void listen(SocketAddress address);

    /**
     * Bind the server host and port.
     *
     * @param host The server host.
     * @param port The server port.
     */
    default void listen(String host, int port) {
        listen(new InetSocketAddress(host, port));
    }
}
