package com.firefly.client.http2;

import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class HTTPClientSingleton extends AbstractLifeCycle {

    private static HTTPClientSingleton ourInstance = new HTTPClientSingleton();

    public static HTTPClientSingleton getInstance() {
        return ourInstance;
    }

    private SimpleHTTPClient httpClient;

    private HTTPClientSingleton() {
        start();
    }

    public SimpleHTTPClient httpClient() {
        return httpClient;
    }

    protected void init() {
        httpClient = new SimpleHTTPClient();
    }

    protected void destroy() {
        if (httpClient != null) {
            httpClient.stop();
            httpClient = null;
        }
    }
}
