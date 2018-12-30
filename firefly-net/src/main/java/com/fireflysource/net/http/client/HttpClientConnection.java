package com.fireflysource.net.http.client;

import com.fireflysource.net.http.common.HttpConnection;

import java.util.concurrent.CompletableFuture;

/**
 * The HTTP connection manager creates the HTTP client connection.
 * If the TLS is enabled, the connection uses the ALPN to decide the HTTP version,
 * or else the connection uses the HTTP Upgrade mechanism to decide the HTTP version.
 * The HTTP2 is the default preferred protocol.
 *
 * @author Pengtao Qiu
 */
public interface HttpClientConnection extends HttpConnection {

    /**
     * Send HTTP request to the remote endpoint.
     *
     * @param request The HTTP request.
     * @return The HTTP response.
     */
    CompletableFuture<HttpClientResponse> send(HttpClientRequest request);

}
