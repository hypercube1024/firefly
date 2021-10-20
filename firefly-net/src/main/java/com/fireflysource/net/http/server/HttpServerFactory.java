package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.HttpConfig;
import com.fireflysource.net.http.server.impl.AsyncHttpProxy;
import com.fireflysource.net.http.server.impl.AsyncHttpServer;

abstract public class HttpServerFactory {

    public static HttpServer create(HttpConfig config) {
        return new AsyncHttpServer(config);
    }

    public static HttpServer create() {
        return new AsyncHttpServer();
    }

    public static HttpProxy createHttpProxy() {
        return new AsyncHttpProxy();
    }

    public static HttpProxy createHttpProxy(HttpConfig config) {
        return new AsyncHttpProxy(config);
    }
}
