package com.firefly.reactive.adapter.http;

import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class ReactiveHTTPsClientSingleton extends AbstractLifeCycle {
    private static ReactiveHTTPsClientSingleton ourInstance = new ReactiveHTTPsClientSingleton();

    public static ReactiveHTTPsClientSingleton getInstance() {
        return ourInstance;
    }

    private ReactiveHTTPClient httpClient;

    private ReactiveHTTPsClientSingleton() {
        start();
    }

    public ReactiveHTTPClient httpsClient() {
        return httpClient;
    }

    @Override
    protected void init() {
        SimpleHTTPClientConfiguration configuration = new SimpleHTTPClientConfiguration();
        configuration.setSecureConnectionEnabled(true);
        httpClient = new ReactiveHTTPClient(configuration);
    }

    @Override
    protected void destroy() {
        if (httpClient != null) {
            httpClient.stop();
            httpClient = null;
        }
    }
}
