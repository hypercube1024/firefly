package com.firefly.client.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class ClearTextHTTP2ClientSingleton extends AbstractLifeCycle {

    private static ClearTextHTTP2ClientSingleton ourInstance = new ClearTextHTTP2ClientSingleton();

    public static ClearTextHTTP2ClientSingleton getInstance() {
        return ourInstance;
    }

    private SimpleHTTPClient httpClient;

    private ClearTextHTTP2ClientSingleton() {
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
