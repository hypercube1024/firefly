package com.firefly.reactive.adapter.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.model.*;
import com.firefly.utils.StringUtils;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * SimpleHTTPClient reactor adapter
 *
 * @author Pengtao Qiu
 */
public class ReactiveHTTPClient extends SimpleHTTPClient {

    public ReactiveHTTPClient() {
        super();
    }

    public ReactiveHTTPClient(SimpleHTTPClientConfiguration http2Configuration) {
        super(http2Configuration);
    }

    public class ReactiveRequestBuilder extends RequestBuilder {

        public ReactiveRequestBuilder() {
            super();
        }

        public ReactiveRequestBuilder(String host, int port, MetaData.Request request) {
            super(host, port, request);
        }

        public Mono<SimpleResponse> toMono() {
            return Mono.fromCompletionStage(toFuture());
        }

    }

    @Override
    public ReactiveRequestBuilder get(String url) {
        return request(HttpMethod.GET.asString(), url);
    }

    @Override
    public ReactiveRequestBuilder post(String url) {
        return request(HttpMethod.POST.asString(), url);
    }

    @Override
    public ReactiveRequestBuilder head(String url) {
        return request(HttpMethod.HEAD.asString(), url);
    }

    @Override
    public ReactiveRequestBuilder put(String url) {
        return request(HttpMethod.PUT.asString(), url);
    }

    @Override
    public ReactiveRequestBuilder delete(String url) {
        return request(HttpMethod.DELETE.asString(), url);
    }

    @Override
    public ReactiveRequestBuilder request(HttpMethod method, String url) {
        return request(method.asString(), url);
    }

    @Override
    public ReactiveRequestBuilder request(String method, String url) {
        try {
            return request(method, new URL(url));
        } catch (MalformedURLException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public ReactiveRequestBuilder request(String method, URL url) {
        try {
            String host = url.getHost();
            int port = url.getPort() < 0 ? url.getDefaultPort() : url.getPort();
            HttpURI httpURI = new HttpURI(url.toURI());
            if (!StringUtils.hasText(httpURI.getPath().trim())) {
                httpURI.setPath("/");
            }
            MetaData.Request request = new MetaData.Request(method, httpURI, HttpVersion.HTTP_1_1, new HttpFields());
            return new ReactiveRequestBuilder(host, port, request);
        } catch (URISyntaxException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }
}
