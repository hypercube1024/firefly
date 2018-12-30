package com.fireflysource.net.http.common;

import com.fireflysource.net.http.common.model.HttpVersion;
import kotlinx.coroutines.CoroutineDispatcher;

/**
 * The HTTP connection.
 *
 * @author Pengtao Qiu
 */
public interface HttpConnection {

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

    /**
     * Get the coroutine dispatcher of this connection. One TCP connection is always in the same coroutine context.
     *
     * @return The coroutine dispatcher of this connection.
     */
    CoroutineDispatcher getCoroutineDispatcher();
}
