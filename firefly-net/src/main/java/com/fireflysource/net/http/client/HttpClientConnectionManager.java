package com.fireflysource.net.http.client;

import java.util.concurrent.CompletableFuture;

/**
 * The HTTP client connection manager maintains the HTTP client connections.
 * If it creates connection and negotiates the HTTP version is 2.0, and it maintains only one connection to send the request and receive the response.
 * If the HTTP version is 1.1, it maintains the  HTTP connections in a pool.
 *
 * @author Pengtao Qiu
 */
public interface HttpClientConnectionManager {

    /**
     * Get HTTP client connection.
     *
     * @param request The HTTP request.
     * @return The HTTP client connection.
     */
    CompletableFuture<HttpClientConnection> getConnection(HttpClientRequest request);

}
