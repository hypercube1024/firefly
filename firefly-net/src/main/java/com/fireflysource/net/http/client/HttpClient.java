package com.fireflysource.net.http.client;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.http.common.model.HttpURI;
import com.fireflysource.net.websocket.client.WebSocketClientConnectionBuilder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface HttpClient extends HttpClientRequestBuilderFactory, LifeCycle {

    /**
     * Create a new HTTP client connection.
     *
     * @param httpURI            The HTTP URI.
     * @param supportedProtocols The supported application protocols.
     * @return The new HTTP client connection.
     */
    CompletableFuture<HttpClientConnection> createHttpClientConnection(HttpURI httpURI, List<String> supportedProtocols);

    /**
     * Create a new HTTP client connection.
     *
     * @param httpURI The HTTP URI.
     * @return The new HTTP client connection.
     */
    default CompletableFuture<HttpClientConnection> createHttpClientConnection(HttpURI httpURI) {
        return createHttpClientConnection(httpURI, Collections.emptyList());
    }

    /**
     * Create a new HTTP client connection.
     *
     * @param uri The HTTP URI.
     * @return The new HTTP client connection.
     */
    default CompletableFuture<HttpClientConnection> createHttpClientConnection(String uri) {
        return createHttpClientConnection(new HttpURI(uri));
    }

    /**
     * Create a websocket connection builder.
     *
     * @return The websocket connection builder.
     */
    WebSocketClientConnectionBuilder websocket();

    /**
     * Create a websocket connection builder.
     *
     * @param url The websocket url.
     * @return The websocket connection builder.
     */
    WebSocketClientConnectionBuilder websocket(String url);
}
