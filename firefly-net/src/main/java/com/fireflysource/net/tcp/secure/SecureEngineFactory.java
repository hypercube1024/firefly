package com.fireflysource.net.tcp.secure;

import com.fireflysource.net.tcp.TcpConnection;

import java.util.List;

/**
 * The TLS engine factory.
 *
 * @author Pengtao Qiu
 */
public interface SecureEngineFactory {

    /**
     * Create a TLS engine.
     *
     * @param connection         The tcp connection.
     * @param clientMode         If true, the current connection is the client tcp connection.
     * @param supportedProtocols The supported application layer protocols.
     * @return The TLS engine.
     */
    SecureEngine create(TcpConnection connection, boolean clientMode, List<String> supportedProtocols);

    /**
     * Create a TLS engine using advisory peer information.
     * Applications using this factory method are providing hints for an internal session reuse strategy.
     * Some cipher suites (such as Kerberos) require remote hostname information, in which case peerHost needs to be specified.
     *
     * @param connection         The tcp connection.
     * @param clientMode         If true, the current connection is the client tcp connection.
     * @param peerHost           the non-authoritative name of the host.
     * @param peerPort           the non-authoritative port.
     * @param supportedProtocols The supported application layer protocols.
     * @return The TLS engine.
     */
    SecureEngine create(TcpConnection connection, boolean clientMode, String peerHost, int peerPort,
                        List<String> supportedProtocols);

}
