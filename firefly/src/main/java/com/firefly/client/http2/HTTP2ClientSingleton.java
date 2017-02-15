package com.firefly.client.http2;

/**
 * @author Pengtao Qiu
 */
public class HTTP2ClientSingleton {

    private static HTTP2ClientSingleton ourInstance = new HTTP2ClientSingleton();

    public static HTTP2ClientSingleton getInstance() {
        return ourInstance;
    }

    private SimpleHTTPClient httpClient;
    private volatile SimpleHTTPClientConfiguration configuration;

    private HTTP2ClientSingleton() {
        init();
    }

    public synchronized SimpleHTTPClient httpClient() {
        init();
        return httpClient;
    }

    public HTTP2ClientSingleton config(SimpleHTTPClientConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    private HTTP2ClientSingleton init() {
        if (httpClient != null) {
            if (httpClient.isStopped()) {
                create();
            }
            return this;
        } else {
            create();
            return this;
        }
    }

    private void create() {
        if (configuration != null) {
            httpClient = new SimpleHTTPClient(configuration);
        } else {
            httpClient = new SimpleHTTPClient();
        }
    }

    public synchronized HTTP2ClientSingleton destroy() {
        if (httpClient != null) {
            httpClient.stop();
            httpClient = null;
        }
        return this;
    }
}
