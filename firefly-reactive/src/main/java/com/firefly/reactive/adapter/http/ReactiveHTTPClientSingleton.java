package com.firefly.reactive.adapter.http;

import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class ReactiveHTTPClientSingleton extends AbstractLifeCycle {
    private static ReactiveHTTPClientSingleton ourInstance = new ReactiveHTTPClientSingleton();

    public static ReactiveHTTPClientSingleton getInstance() {
        return ourInstance;
    }

    private ReactiveHTTPClient httpClient;

    private ReactiveHTTPClientSingleton() {
        start();
    }

    public ReactiveHTTPClient httpClient() {
        return httpClient;
    }

    @Override
    protected void init() {
        httpClient = new ReactiveHTTPClient();
    }

    @Override
    protected void destroy() {
        if (httpClient != null) {
            httpClient.stop();
            httpClient = null;
        }
    }
}
