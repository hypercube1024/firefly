package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.HttpConfig;
import com.fireflysource.net.http.server.impl.AsyncHttpServer;
import com.fireflysource.net.http.server.impl.HttpProxy;

abstract public class HttpServerFactory {

    public static HttpServer create(HttpConfig config) {
        return new AsyncHttpServer(config);
    }

    public static HttpServer create() {
        return new AsyncHttpServer();
    }

    public static HttpProxy createHttpProxy() {
        return new HttpProxy();
    }

    public static HttpProxy createHttpProxy(HttpConfig config) {
        return new HttpProxy(config);
    }
}
