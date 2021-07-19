package com.fireflysource.net.tcp;

/**
 * The wrapped TCP connection.
 *
 * @author Pengtao Qiu
 */
public interface WrappedTcpConnection {

    /**
     * Get the raw TCP connection.
     *
     * @return The raw TCP connection.
     */
    TcpConnection getRawTcpConnection();
}
