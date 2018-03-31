package com.firefly.reactive.adapter.http;

import com.firefly.client.http2.HTTPClientConnection;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action3;
import com.firefly.utils.function.Func1;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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

        @Override
        public ReactiveRequestBuilder cookies(List<Cookie> cookies) {
            super.cookies(cookies);
            return this;
        }

        @Override
        public ReactiveRequestBuilder put(String name, List<String> list) {
            super.put(name, list);
            return this;
        }

        @Override
        public ReactiveRequestBuilder put(HttpHeader header, String value) {
            super.put(header, value);
            return this;
        }

        @Override
        public ReactiveRequestBuilder put(String name, String value) {
            super.put(name, value);
            return this;
        }

        @Override
        public ReactiveRequestBuilder put(HttpField field) {
            super.put(field);
            return this;
        }

        @Override
        public ReactiveRequestBuilder addAll(HttpFields fields) {
            super.addAll(fields);
            return this;
        }

        @Override
        public ReactiveRequestBuilder add(HttpField field) {
            super.add(field);
            return this;
        }

        @Override
        public ReactiveRequestBuilder setTrailerSupplier(Supplier<HttpFields> trailers) {
            super.setTrailerSupplier(trailers);
            return this;
        }

        @Override
        public ReactiveRequestBuilder jsonBody(Object obj) {
            super.jsonBody(obj);
            return this;
        }

        @Override
        public ReactiveRequestBuilder body(String content) {
            super.body(content);
            return this;
        }

        @Override
        public ReactiveRequestBuilder body(String content, Charset charset) {
            super.body(content, charset);
            return this;
        }

        @Override
        public ReactiveRequestBuilder write(ByteBuffer buffer) {
            super.write(buffer);
            return this;
        }

        @Override
        public ReactiveRequestBuilder output(Action1<HTTPOutputStream> output) {
            super.output(output);
            return this;
        }

        @Override
        public ReactiveRequestBuilder output(Promise<HTTPOutputStream> promise) {
            super.output(promise);
            return this;
        }

        @Override
        public ReactiveRequestBuilder addFieldPart(String name, ContentProvider content, HttpFields fields) {
            super.addFieldPart(name, content, fields);
            return this;
        }

        @Override
        public ReactiveRequestBuilder addFilePart(String name, String fileName, ContentProvider content, HttpFields fields) {
            super.addFilePart(name, fileName, content, fields);
            return this;
        }

        @Override
        public ReactiveRequestBuilder addFormParam(String name, String value) {
            super.addFormParam(name, value);
            return this;
        }

        @Override
        public ReactiveRequestBuilder addFormParam(String name, List<String> values) {
            super.addFormParam(name, values);
            return this;
        }

        @Override
        public ReactiveRequestBuilder putFormParam(String name, String value) {
            super.putFormParam(name, value);
            return this;
        }

        @Override
        public ReactiveRequestBuilder putFormParam(String name, List<String> values) {
            super.putFormParam(name, values);
            return this;
        }

        @Override
        public ReactiveRequestBuilder removeFormParam(String name) {
            super.removeFormParam(name);
            return this;
        }

        @Override
        public ReactiveRequestBuilder connect(Func1<HTTPClientConnection, CompletableFuture<Boolean>> connect) {
            super.connect(connect);
            return this;
        }

        @Override
        public ReactiveRequestBuilder headerComplete(Action1<MetaData.Response> headerComplete) {
            super.headerComplete(headerComplete);
            return this;
        }

        @Override
        public ReactiveRequestBuilder messageComplete(Action1<MetaData.Response> messageComplete) {
            super.messageComplete(messageComplete);
            return this;
        }

        @Override
        public ReactiveRequestBuilder content(Action1<ByteBuffer> content) {
            super.content(content);
            return this;
        }

        @Override
        public ReactiveRequestBuilder contentComplete(Action1<MetaData.Response> contentComplete) {
            super.contentComplete(contentComplete);
            return this;
        }

        @Override
        public ReactiveRequestBuilder badMessage(Action3<Integer, String, MetaData.Response> badMessage) {
            super.badMessage(badMessage);
            return this;
        }

        @Override
        public ReactiveRequestBuilder earlyEof(Action1<MetaData.Response> earlyEof) {
            super.earlyEof(earlyEof);
            return this;
        }

        @Override
        public ReactiveRequestBuilder settings(SettingsFrame settingsFrame) {
            super.settings(settingsFrame);
            return this;
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
