package com.firefly.client.http2;

import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class HTTPsClientSingleton extends AbstractLifeCycle {
    private static HTTPsClientSingleton ourInstance = new HTTPsClientSingleton();

    public static HTTPsClientSingleton getInstance() {
        return ourInstance;
    }

    private SimpleHTTPClient httpClient;

    private HTTPsClientSingleton() {
        start();
    }

    public SimpleHTTPClient httpsClient() {
        return httpClient;
    }

    @Override
    protected void init() {
        SimpleHTTPClientConfiguration configuration = new SimpleHTTPClientConfiguration();
        configuration.setSecureConnectionEnabled(true);
        httpClient = new SimpleHTTPClient(configuration);
    }

    @Override
    protected void destroy() {
        if (httpClient != null) {
            httpClient.stop();
            httpClient = null;
        }
    }
}
