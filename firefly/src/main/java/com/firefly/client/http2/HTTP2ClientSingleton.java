package com.firefly.client.http2;

import com.firefly.net.SSLContextFactory;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class HTTP2ClientSingleton extends AbstractLifeCycle {

    private static HTTP2ClientSingleton ourInstance = new HTTP2ClientSingleton();

    public static HTTP2ClientSingleton getInstance() {
        return ourInstance;
    }

    private SimpleHTTPClient httpClient;
    private volatile SimpleHTTPClientConfiguration configuration;

    private HTTP2ClientSingleton() {
    }

    public SimpleHTTPClient httpClient() {
        start();
        return httpClient;
    }

    public SimpleHTTPClient httpsClient() {
        SimpleHTTPClientConfiguration configuration = new SimpleHTTPClientConfiguration();
        configuration.setSecureConnectionEnabled(true);
        config(configuration);
        start();
        return httpClient;
    }

    public SimpleHTTPClient httpsClient(SSLContextFactory sslContextFactory) {
        SimpleHTTPClientConfiguration configuration = new SimpleHTTPClientConfiguration();
        configuration.setSecureConnectionEnabled(true);
        configuration.setSslContextFactory(sslContextFactory);
        config(configuration);
        start();
        return httpClient;
    }

    public HTTP2ClientSingleton config(SimpleHTTPClientConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    protected void init() {
        if (configuration != null) {
            httpClient = new SimpleHTTPClient(configuration);
        } else {
            httpClient = new SimpleHTTPClient();
        }
    }

    protected void destroy() {
        if (httpClient != null) {
            httpClient.stop();
            httpClient = null;
        }
    }
}
