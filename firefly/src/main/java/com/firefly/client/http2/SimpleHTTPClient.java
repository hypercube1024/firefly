package com.firefly.client.http2;

import com.codahale.metrics.*;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action3;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.EofException;
import com.firefly.utils.io.IO;
import com.firefly.utils.json.Json;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.pool.AsynchronousPool;
import com.firefly.utils.lang.pool.BoundedAsynchronousPool;
import com.firefly.utils.lang.pool.PooledObject;
import com.firefly.utils.time.Millisecond100Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SimpleHTTPClient extends AbstractLifeCycle {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final HTTP2Client http2Client;
    private final ConcurrentHashMap<RequestBuilder, AsynchronousPool<HTTPClientConnection>> poolMap = new ConcurrentHashMap<>();
    private final SimpleHTTPClientConfiguration simpleHTTPClientConfiguration;
    private final Timer responseTimer;
    private final Meter errorMeter;

    public SimpleHTTPClient() {
        this(new SimpleHTTPClientConfiguration());
    }

    public SimpleHTTPClient(SimpleHTTPClientConfiguration http2Configuration) {
        this.simpleHTTPClientConfiguration = http2Configuration;
        http2Client = new HTTP2Client(http2Configuration);
        MetricRegistry metrics = http2Configuration.getTcpConfiguration().getMetrics();
        responseTimer = metrics.timer("http2.SimpleHTTPClient.response.time");
        errorMeter = metrics.meter("http2.SimpleHTTPClient.error.count");
        metrics.register("http2.SimpleHTTPClient.error.ratio.1m", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(errorMeter.getOneMinuteRate(), responseTimer.getOneMinuteRate());
            }
        });
        start();
    }

    public class RequestBuilder {
        String host;
        int port;

        MetaData.Request request;
        List<ByteBuffer> requestBody = new ArrayList<>();

        Action1<Response> headerComplete;
        Action1<ByteBuffer> content;
        Action1<Response> contentComplete;
        Action1<Response> messageComplete;

        Action3<Integer, String, Response> badMessage;
        Action1<Response> earlyEof;

        Promise<HTTPOutputStream> promise;
        Action1<HTTPOutputStream> output;
        MultiPartContentProvider multiPartProvider;
        UrlEncoded formUrlEncoded;

        Promise.Completable<SimpleResponse> future;
        SimpleResponse simpleResponse;

        public RequestBuilder cookies(List<Cookie> cookies) {
            request.getFields().put(HttpHeader.COOKIE, CookieGenerator.generateCookies(cookies));
            return this;
        }

        public RequestBuilder put(String name, List<String> list) {
            request.getFields().put(name, list);
            return this;
        }

        public RequestBuilder put(HttpHeader header, String value) {
            request.getFields().put(header, value);
            return this;
        }

        public RequestBuilder put(String name, String value) {
            request.getFields().put(name, value);
            return this;
        }

        public RequestBuilder put(HttpField field) {
            request.getFields().put(field);
            return this;
        }

        public RequestBuilder addAll(HttpFields fields) {
            request.getFields().addAll(fields);
            return this;
        }

        public RequestBuilder add(HttpField field) {
            request.getFields().add(field);
            return this;
        }

        public Supplier<HttpFields> getTrailerSupplier() {
            return request.getTrailerSupplier();
        }

        public RequestBuilder setTrailerSupplier(Supplier<HttpFields> trailers) {
            request.setTrailerSupplier(trailers);
            return this;
        }

        public RequestBuilder jsonBody(Object obj) {
            return put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON.asString()).body(Json.toJson(obj));
        }

        public RequestBuilder body(String content) {
            return body(content, StandardCharsets.UTF_8);
        }

        public RequestBuilder body(String content, Charset charset) {
            return write(BufferUtils.toBuffer(content, charset));
        }

        public RequestBuilder write(ByteBuffer buffer) {
            requestBody.add(buffer);
            return this;
        }

        public RequestBuilder output(Action1<HTTPOutputStream> output) {
            this.output = output;
            return this;
        }

        public RequestBuilder output(Promise<HTTPOutputStream> promise) {
            this.promise = promise;
            return this;
        }

        MultiPartContentProvider multiPartProvider() {
            if (multiPartProvider == null) {
                multiPartProvider = new MultiPartContentProvider();
                put(HttpHeader.CONTENT_TYPE, multiPartProvider.getContentType());
            }
            return multiPartProvider;
        }

        public RequestBuilder addFieldPart(String name, ContentProvider content, HttpFields fields) {
            multiPartProvider().addFieldPart(name, content, fields);
            return this;
        }

        public RequestBuilder addFilePart(String name, String fileName, ContentProvider content, HttpFields fields) {
            multiPartProvider().addFilePart(name, fileName, content, fields);
            return this;
        }

        UrlEncoded formUrlEncoded() {
            if (formUrlEncoded == null) {
                formUrlEncoded = new UrlEncoded();
                put(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded");
            }
            return formUrlEncoded;
        }

        public RequestBuilder addFormParam(String name, String value) {
            formUrlEncoded().add(name, value);
            return this;
        }

        public RequestBuilder addFormParam(String name, List<String> values) {
            formUrlEncoded().addValues(name, values);
            return this;
        }

        public RequestBuilder putFormParam(String name, String value) {
            formUrlEncoded().put(name, value);
            return this;
        }

        public RequestBuilder putFormParam(String name, List<String> values) {
            formUrlEncoded().putValues(name, values);
            return this;
        }

        public RequestBuilder removeFormParam(String name) {
            formUrlEncoded().remove(name);
            return this;
        }

        public RequestBuilder headerComplete(Action1<Response> headerComplete) {
            this.headerComplete = headerComplete;
            return this;
        }

        public RequestBuilder messageComplete(Action1<Response> messageComplete) {
            this.messageComplete = messageComplete;
            return this;
        }

        public RequestBuilder content(Action1<ByteBuffer> content) {
            this.content = content;
            return this;
        }

        public RequestBuilder contentComplete(Action1<Response> contentComplete) {
            this.contentComplete = contentComplete;
            return this;
        }

        public RequestBuilder badMessage(Action3<Integer, String, Response> badMessage) {
            this.badMessage = badMessage;
            return this;
        }

        public RequestBuilder earlyEof(Action1<Response> earlyEof) {
            this.earlyEof = earlyEof;
            return this;
        }

        public Promise.Completable<SimpleResponse> submit() {
            submit(new Promise.Completable<>());
            return future;
        }

        public void submit(Promise.Completable<SimpleResponse> future) {
            this.future = future;
            send(this);
        }

        public void submit(Action1<SimpleResponse> action) {
            Promise.Completable<SimpleResponse> future = new Promise.Completable<SimpleResponse>() {
                public void succeeded(SimpleResponse t) {
                    super.succeeded(t);
                    action.call(t);
                }

                public void failed(Throwable c) {
                    super.failed(c);
                    log.error("http request exception", c);
                }
            };
            submit(future);
        }

        public void end() {
            send(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RequestBuilder that = (RequestBuilder) o;
            return port == that.port &&
                    Objects.equals(host, that.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port);
        }

    }

    public void removeConnectionPool(String url) {
        try {
            removeConnectionPool(new URL(url));
        } catch (MalformedURLException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    public void removeConnectionPool(URL url) {
        RequestBuilder req = new RequestBuilder();
        req.host = url.getHost();
        req.port = url.getPort() < 0 ? url.getDefaultPort() : url.getPort();
        removePool(req);
    }

    public void removeConnectionPool(String host, int port) {
        RequestBuilder req = new RequestBuilder();
        req.host = host;
        req.port = port;
        removePool(req);
    }

    private void removePool(RequestBuilder req) {
        AsynchronousPool<HTTPClientConnection> pool = poolMap.remove(req);
        pool.stop();
    }

    public int getConnectionPoolSize(String host, int port) {
        RequestBuilder req = new RequestBuilder();
        req.host = host;
        req.port = port;
        return _getPoolSize(req);
    }

    public int getConnectionPoolSize(String url) {
        try {
            return getConnectionPoolSize(new URL(url));
        } catch (MalformedURLException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    public int getConnectionPoolSize(URL url) {
        RequestBuilder req = new RequestBuilder();
        req.host = url.getHost();
        req.port = url.getPort() < 0 ? url.getDefaultPort() : url.getPort();
        return _getPoolSize(req);
    }

    private int _getPoolSize(RequestBuilder req) {
        AsynchronousPool<HTTPClientConnection> pool = poolMap.get(req);
        if (pool != null) {
            return pool.size();
        } else {
            return 0;
        }
    }

    public RequestBuilder get(String url) {
        return request(HttpMethod.GET.asString(), url);
    }

    public RequestBuilder post(String url) {
        return request(HttpMethod.POST.asString(), url);
    }

    public RequestBuilder head(String url) {
        return request(HttpMethod.HEAD.asString(), url);
    }

    public RequestBuilder put(String url) {
        return request(HttpMethod.PUT.asString(), url);
    }

    public RequestBuilder delete(String url) {
        return request(HttpMethod.DELETE.asString(), url);
    }

    public RequestBuilder request(HttpMethod method, String url) {
        return request(method.asString(), url);
    }

    public RequestBuilder request(String method, String url) {
        try {
            return request(method, new URL(url));
        } catch (MalformedURLException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    public RequestBuilder request(String method, URL url) {
        try {
            RequestBuilder req = new RequestBuilder();
            req.host = url.getHost();
            req.port = url.getPort() < 0 ? url.getDefaultPort() : url.getPort();
            req.request = new MetaData.Request(method, new HttpURI(url.toURI()), HttpVersion.HTTP_1_1,
                    new HttpFields());
            return req;
        } catch (URISyntaxException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    protected void send(RequestBuilder r) {
        Timer.Context resTimerCtx = responseTimer.time();
        AsynchronousPool<HTTPClientConnection> pool = getPool(r);
        pool.take().thenAccept(o -> {
            HTTPClientConnection connection = o.getObject();
            connection.close(conn -> pool.release(o))
                      .exception((conn, exception) -> pool.release(o));

            if (connection.getHttpVersion() == HttpVersion.HTTP_2) {
                pool.release(o);
            }

            log.debug("take the connection {} from pool, released: {}",
                    connection.getSessionId(),
                    o.isReleased());

            ClientHTTPHandler handler = new ClientHTTPHandler.Adapter()
                    .headerComplete((req, resp, outputStream, conn) -> {
                        if (r.headerComplete != null) {
                            r.headerComplete.call(resp);
                        }
                        if (r.future != null) {
                            if (r.simpleResponse == null) {
                                r.simpleResponse = new SimpleResponse(resp);
                            }
                        }
                        return false;
                    }).content((buffer, req, resp, outputStream, conn) -> {
                        if (r.content != null) {
                            r.content.call(buffer);
                        }
                        if (r.future != null && r.simpleResponse != null) {
                            r.simpleResponse.responseBody.add(buffer);
                        }
                        return false;
                    }).contentComplete((req, resp, outputStream, conn) -> {
                        if (r.contentComplete != null) {
                            r.contentComplete.call(resp);
                        }
                        return false;
                    }).messageComplete((req, resp, outputStream, conn) -> {
                        pool.release(o);
                        log.debug("complete request of the connection {} , released: {}",
                                connection.getSessionId(),
                                o.isReleased());
                        if (r.messageComplete != null) {
                            r.messageComplete.call(resp);
                        }
                        if (r.future != null) {
                            r.future.succeeded(r.simpleResponse);
                        }
                        resTimerCtx.stop();
                        return true;
                    }).badMessage((errCode, reason, req, resp, outputStream, conn) -> {
                        pool.release(o);
                        log.debug("bad message of the connection {} , released: {}",
                                connection.getSessionId(),
                                o.isReleased());
                        if (r.badMessage != null) {
                            r.badMessage.call(errCode, reason, resp);
                        }
                        if (r.future != null) {
                            if (r.simpleResponse == null) {
                                r.simpleResponse = new SimpleResponse(resp);
                            }
                            r.future.failed(new BadMessageException(errCode, reason));
                        }
                        if (r.badMessage == null && r.future == null) {
                            IO.close(o.getObject());
                        }
                        errorMeter.mark();
                        resTimerCtx.stop();
                    }).earlyEOF((req, resp, outputStream, conn) -> {
                        pool.release(o);
                        log.debug("eafly EOF of the connection {} , released: {}",
                                connection.getSessionId(),
                                o.isReleased());
                        if (r.earlyEof != null) {
                            r.earlyEof.call(resp);
                        }
                        if (r.future != null) {
                            if (r.simpleResponse == null) {
                                r.simpleResponse = new SimpleResponse(resp);
                            }
                            r.future.failed(new EofException("early eof"));
                        }
                        if (r.earlyEof == null && r.future == null) {
                            IO.close(o.getObject());
                        }
                        errorMeter.mark();
                        resTimerCtx.stop();
                    });

            if (r.requestBody != null && !r.requestBody.isEmpty()) {
                connection.send(r.request, r.requestBody.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), handler);
            } else if (r.promise != null) {
                connection.send(r.request, r.promise, handler);
            } else if (r.output != null) {
                Promise<HTTPOutputStream> p = new Promise<HTTPOutputStream>() {
                    public void succeeded(HTTPOutputStream out) {
                        r.output.call(out);
                    }
                };
                connection.send(r.request, p, handler);
            } else if (r.multiPartProvider != null) {
                IO.close(r.multiPartProvider);
                r.multiPartProvider.setListener(() -> log.debug("multi part content listener"));
                if (r.multiPartProvider.getLength() > 0) {
                    r.put(HttpHeader.CONTENT_LENGTH, String.valueOf(r.multiPartProvider.getLength()));
                }
                Promise.Completable<HTTPOutputStream> p = new Promise.Completable<>();
                connection.send(r.request, p, handler);
                p.thenAccept(output -> {
                    try (HTTPOutputStream out = output) {
                        for (ByteBuffer buf : r.multiPartProvider) {
                            out.write(buf);
                        }
                    } catch (IOException e) {
                        log.error("SimpleHTTPClient writes data exception", e);
                    }
                }).exceptionally(t -> {
                    log.error("SimpleHTTPClient gets output stream exception", t);
                    resTimerCtx.stop();
                    errorMeter.mark();
                    return null;
                });
            } else if (r.formUrlEncoded != null) {
                String body = r.formUrlEncoded.encode(Charset.forName(simpleHTTPClientConfiguration.getCharacterEncoding()), true);
                byte[] content = StringUtils.getBytes(body, simpleHTTPClientConfiguration.getCharacterEncoding());
                connection.send(r.request, ByteBuffer.wrap(content), handler);
            } else {
                connection.send(r.request, handler);
            }
        }).exceptionally(e -> {
            log.error("SimpleHTTPClient sends message exception", e);
            resTimerCtx.stop();
            errorMeter.mark();
            return null;
        });
    }

    private AsynchronousPool<HTTPClientConnection> getPool(RequestBuilder request) {
        return poolMap.computeIfAbsent(request,
                req -> new BoundedAsynchronousPool<>(simpleHTTPClientConfiguration.getPoolSize(),
                        simpleHTTPClientConfiguration.getConnectTimeout(),
                        () -> {
                            Promise.Completable<PooledObject<HTTPClientConnection>> r = new Promise.Completable<>();
                            Promise.Completable<HTTPClientConnection> c = http2Client.connect(request.host, request.port);
                            c.thenAccept(conn -> r.succeeded(new PooledObject<>(conn)))
                             .exceptionally(e -> {
                                 r.failed(e);
                                 return null;
                             });
                            return r;
                        },
                        o -> o.getObject().isOpen(),
                        (o) -> {
                            try {
                                o.getObject().close();
                            } catch (IOException e) {
                                log.error("close http connection exception", e);
                            }
                        }));
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {
        http2Client.stop();
        poolMap.forEach((k, v) -> v.stop());
    }
}
