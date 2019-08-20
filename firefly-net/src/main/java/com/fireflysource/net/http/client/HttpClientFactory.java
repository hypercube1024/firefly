package com.fireflysource.net.http.client;

import com.fireflysource.net.http.client.impl.AsyncHttpClient;
import com.fireflysource.net.http.common.HttpConfig;

abstract public class HttpClientFactory {

    public static HttpClient create(HttpConfig config) {
        return new AsyncHttpClient(config);
    }

    public static HttpClient create() {
        return new AsyncHttpClient();
    }
}
