package com.fireflysource.net.http.client;

import com.fireflysource.net.http.client.impl.AsyncHttpClient;

abstract public class HttpClientFactory {

    public static HttpClient create(HttpClientConfig config) {
        return new AsyncHttpClient(config);
    }

    public static HttpClient create() {
        return new AsyncHttpClient();
    }
}
