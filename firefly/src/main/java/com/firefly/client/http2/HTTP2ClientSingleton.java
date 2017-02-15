package com.firefly.client.http2;

/**
 * @author Pengtao Qiu
 */
public class HTTP2ClientSingleton {

    private static HTTP2ClientSingleton ourInstance = new HTTP2ClientSingleton();

    public static HTTP2ClientSingleton getInstance() {
        return ourInstance;
    }

    private volatile SimpleHTTPClient httpClient;
    private SimpleHTTPClientConfiguration configuration;

    private HTTP2ClientSingleton() {
        init();
    }

    public SimpleHTTPClient httpClient() {
        if (httpClient == null) {
            throw new IllegalStateException("the http client has not been initialized");
        }

        if (httpClient.isStopped()) {
            httpClient = null;
            throw new IllegalStateException("the http client has stopped, please call init() again");
        }
        return httpClient;
    }

    public HTTP2ClientSingleton config(SimpleHTTPClientConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    public HTTP2ClientSingleton init() {
        if (httpClient != null) {
            return this;
        } else {
            if (configuration != null) {
                httpClient = new SimpleHTTPClient(configuration);
            } else {
                httpClient = new SimpleHTTPClient();
            }
            return this;
        }
    }

    public HTTP2ClientSingleton destroy() {
        if (httpClient != null) {
            httpClient.stop();
            httpClient = null;
        }
        return this;
    }
}
