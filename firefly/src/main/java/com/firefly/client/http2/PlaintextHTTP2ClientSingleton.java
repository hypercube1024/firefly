package com.firefly.client.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class PlaintextHTTP2ClientSingleton extends AbstractLifeCycle {

    private static PlaintextHTTP2ClientSingleton ourInstance = new PlaintextHTTP2ClientSingleton();

    public static PlaintextHTTP2ClientSingleton getInstance() {
        return ourInstance;
    }

    private SimpleHTTPClient httpClient;

    private PlaintextHTTP2ClientSingleton() {
        start();
    }

    public SimpleHTTPClient httpClient() {
        return httpClient;
    }

    protected void init() {
        SimpleHTTPClientConfiguration configuration = new SimpleHTTPClientConfiguration();
        configuration.setProtocol(HttpVersion.HTTP_2.asString());
        httpClient = new SimpleHTTPClient(configuration);
    }

    protected void destroy() {
        if (httpClient != null) {
            httpClient.stop();
            httpClient = null;
        }
    }
}
