package com.fireflysource.net.http.common;

import com.fireflysource.net.Connection;
import com.fireflysource.net.http.common.model.HttpVersion;
import com.fireflysource.net.tcp.TcpCoroutineDispatcher;

/**
 * The HTTP connection.
 *
 * @author Pengtao Qiu
 */
public interface HttpConnection extends Connection, TcpCoroutineDispatcher {

    /**
     * Get the HTTP version.
     *
     * @return The HTTP version.
     */
    HttpVersion getHttpVersion();

    /**
     * If you enable the TLS protocol, it returns true.
     *
     * @return If you enable the TLS protocol, it returns true.
     */
    boolean isSecureConnection();
}
