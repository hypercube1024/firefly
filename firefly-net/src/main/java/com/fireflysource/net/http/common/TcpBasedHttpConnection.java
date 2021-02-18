package com.fireflysource.net.http.common;

import com.fireflysource.net.tcp.TcpConnection;

/**
 * The TCP based HTTP connection.
 *
 * @author Pengtao Qiu
 */
public interface TcpBasedHttpConnection extends HttpConnection {

    /**
     * Get the TCP connection.
     *
     * @return The TCP connection.
     */
    TcpConnection getTcpConnection();
}
