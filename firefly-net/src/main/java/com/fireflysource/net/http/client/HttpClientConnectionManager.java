package com.fireflysource.net.http.client;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.http.common.model.HttpURI;

import java.util.concurrent.CompletableFuture;

/**
 * The HTTP client connection manager maintains the HTTP client connections.
 * If it creates connection and negotiates the HTTP version is 2.0, and it maintains only one connection to send the request and receive the response.
 * If the HTTP version is 1.1, it maintains the  HTTP connections in a pool.
 *
 * @author Pengtao Qiu
 */
public interface HttpClientConnectionManager extends LifeCycle {

    /**
     * Send HTTP request to the server using the connection pool.
     *
     * @param request The HTTP request.
     * @return The HTTP response.
     */
    CompletableFuture<HttpClientResponse> send(HttpClientRequest request);

    /**
     * Create a new HTTP client connection.
     *
     * @param httpURI The server URI.
     * @return The new HTTP client connection.
     */
    CompletableFuture<HttpClientConnection> createHttpClientConnection(HttpURI httpURI);

}
