package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.model.HttpMethod;

import java.util.List;

public interface HttpServerBuilder {

    /**
     * Bind a URL for this router.
     *
     * @param url The URL.
     * @return The HTTP server build.
     */
    HttpServerBuilder path(String url);

    /**
     * Bind some URLs for this router.
     *
     * @param urlList The URL list.
     * @return The HTTP server build.
     */
    HttpServerBuilder paths(List<String> urlList);

    /**
     * Bind URL using regex.
     *
     * @param regex The URL regex.
     * @return The HTTP server build.
     */
    HttpServerBuilder pathRegex(String regex);

    /**
     * Bind HTTP method.
     *
     * @param httpMethod The HTTP method.
     * @return The HTTP server build.
     */
    HttpServerBuilder method(String httpMethod);

    /**
     * Bind HTTP method.
     *
     * @param httpMethod The HTTP method.
     * @return The HTTP server build.
     */
    HttpServerBuilder method(HttpMethod httpMethod);

    /**
     * Bind get method and URL.
     *
     * @param url The URL.
     * @return The HTTP server build.
     */
    HttpServerBuilder get(String url);

    /**
     * Bind post method and URL.
     *
     * @param url The URL.
     * @return The HTTP server build.
     */
    HttpServerBuilder post(String url);

    /**
     * Bind put method and URL.
     *
     * @param url The URL.
     * @return The HTTP server build.
     */
    HttpServerBuilder put(String url);

    /**
     * Bind delete method and URL.
     *
     * @param url The URL.
     * @return The HTTP server build.
     */
    HttpServerBuilder delete(String url);

    /**
     * Bind the request content type.
     *
     * @param contentType The request content type.
     * @return The HTTP server build.
     */
    HttpServerBuilder consumes(String contentType);

    /**
     * Bind remote accepted content type.
     *
     * @param accept The remote accepted content type.
     * @return The HTTP server build.
     */
    HttpServerBuilder produces(String accept);

    /**
     * Set router handler. When the HTTP server accepted request, and the request match this router,
     * the server will call this handler to process request.
     *
     * @param handler router handler.
     * @return The HTTP server build.
     */
    HttpServerBuilder handler(Router.Handler handler);

    /**
     * Enable this router.
     *
     * @return The HTTP server build.
     */
    HttpServerBuilder enable();

    /**
     * Disable this router.
     *
     * @return The HTTP server build.
     */
    HttpServerBuilder disable();

    /**
     * Start HTTP server listener.
     *
     * @param host The host.
     * @param port The port.
     */
    void listen(String host, int port);

    /**
     * Start HTTP server listener.
     */
    void listen();
}
