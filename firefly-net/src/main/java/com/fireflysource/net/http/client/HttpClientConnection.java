package com.fireflysource.net.http.client;

import com.fireflysource.net.Connection;
import com.fireflysource.net.http.common.model.HttpVersion;
import kotlinx.coroutines.CoroutineDispatcher;

import java.util.concurrent.CompletableFuture;

/**
 * The HTTP connection manager creates the HTTP client connection.
 * If the TLS is enabled, the connection uses the ALPN to decide the HTTP version,
 * or else the connection uses the HTTP Upgrade mechanism to decide the HTTP version.
 * The HTTP2 is the default preferred protocol.
 *
 * @author Pengtao Qiu
 */
public interface HttpClientConnection extends Connection {

    /**
     * Send HTTP request to the remote endpoint.
     *
     * @param request The HTTP request.
     * @return The HTTP response.
     */
    CompletableFuture<HttpClientResponse> send(HttpClientRequest request);

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
