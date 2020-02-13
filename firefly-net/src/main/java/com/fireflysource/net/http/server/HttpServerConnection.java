package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.HttpConnection;

import java.util.function.Consumer;

/**
 * The HTTP server connection.
 *
 * @author Pengtao Qiu
 */
public interface HttpServerConnection extends HttpConnection {

    HttpServerConnection onHeaderComplete(Consumer<RoutingContext> consumer);

    HttpServerConnection onHttpRequestComplete(Consumer<RoutingContext> consumer);

    void begin();

}
