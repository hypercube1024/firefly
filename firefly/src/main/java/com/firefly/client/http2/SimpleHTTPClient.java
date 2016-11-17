package com.firefly.client.http2;

import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action3;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.EofException;
import com.firefly.utils.json.Json;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.pool.BlockingPool;
import com.firefly.utils.lang.pool.BoundedBlockingPool;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SimpleHTTPClient extends AbstractLifeCycle {

    private static Logger log = LoggerFactory.getLogger("firefly-system");
    private static Logger monitor = LoggerFactory.getLogger("firefly-monitor");

    protected final HTTP2Client http2Client;

    private final Map<RequestBuilder, BlockingPool<HTTPClientConnection>> poolMap = new ConcurrentReferenceHashMap<>();

    public SimpleHTTPClient() {
        this(new SimpleHTTPClientConfiguration());
    }

    public SimpleHTTPClient(SimpleHTTPClientConfiguration http2Configuration) {
        http2Client = new HTTP2Client(http2Configuration);
        start();
    }

    public class RequestBuilder {
        String host;
        int port;

        MetaData.Request request;
        List<ByteBuffer> requestBody = new ArrayList<>();

        Action1<Response> headerComplete;
        Action1<ByteBuffer> content;
        Action1<Response> messageComplete;

        Action3<Integer, String, Response> badMessage;
        Action1<Response> earlyEof;

        Promise<HTTPOutputStream> promise;
        Action1<HTTPOutputStream> output;

        FuturePromise<SimpleResponse> future;
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

        public RequestBuilder badMessage(Action3<Integer, String, Response> badMessage) {
            this.badMessage = badMessage;
            return this;
        }

        public RequestBuilder earlyEof(Action1<Response> earlyEof) {
            this.earlyEof = earlyEof;
            return this;
        }

        public FuturePromise<SimpleResponse> submit() {
            submit(new FuturePromise<>());
            return future;
        }

        public void submit(FuturePromise<SimpleResponse> future) {
            this.future = future;
            send(this);
        }

        public void submit(Action1<SimpleResponse> action) {
            FuturePromise<SimpleResponse> future = new FuturePromise<SimpleResponse>() {
                public void succeeded(SimpleResponse t) {
                    action.call(t);
                }

                public void failed(Throwable c) {
                    log.error("http request exception", c);
                }
            };
            submit(future);
        }

        public void end() {
            send(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((host == null) ? 0 : host.hashCode());
            result = prime * result + port;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RequestBuilder other = (RequestBuilder) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (host == null) {
                if (other.host != null)
                    return false;
            } else if (!host.equals(other.host))
                return false;
            if (port != other.port)
                return false;
            return true;
        }

        private SimpleHTTPClient getOuterType() {
            return SimpleHTTPClient.this;
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
        poolMap.remove(req);
    }

    public void removeConnectionPool(String host, int port) {
        RequestBuilder req = new RequestBuilder();
        req.host = host;
        req.port = port;
        poolMap.remove(req);
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
        BlockingPool<HTTPClientConnection> pool = poolMap.get(req);
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

    private void release(HTTPClientConnection connection, BlockingPool<HTTPClientConnection> pool) {
        boolean released = (Boolean) connection.getAttachment();
        if (released == false) {
            connection.setAttachment(true);
            pool.release(connection);
        }
    }

    protected void send(RequestBuilder r) {
        long start = Millisecond100Clock.currentTimeMillis();
        SimpleHTTPClientConfiguration config = (SimpleHTTPClientConfiguration) http2Client.getHttp2Configuration();
        BlockingPool<HTTPClientConnection> pool = getPool(r);
        try {
            HTTPClientConnection connection = pool.take(config.getTakeConnectionTimeout(), TimeUnit.MILLISECONDS);
            connection.setAttachment(false);

            if (connection.getHttpVersion() == HttpVersion.HTTP_2) {
                release(connection, pool);
            }

            log.debug("take the connection {} from pool, released: {}", connection.getSessionId(),
                    connection.getAttachment());

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
                    }).messageComplete((req, resp, outputStream, conn) -> {
                        release(connection, pool);
                        log.debug("complete request of the connection {} , released: {}", connection.getSessionId(),
                                connection.getAttachment());
                        if (r.messageComplete != null) {
                            r.messageComplete.call(resp);
                        }
                        if (r.future != null) {
                            r.future.succeeded(r.simpleResponse);
                        }
                        return true;
                    }).content((buffer, req, resp, outputStream, conn) -> {
                        if (r.content != null) {
                            r.content.call(buffer);
                        }
                        if (r.future != null && r.simpleResponse != null) {
                            r.simpleResponse.responseBody.add(buffer);
                        }
                        return false;
                    }).badMessage((errCode, reason, req, resp, outputStream, conn) -> {
                        release(connection, pool);
                        log.debug("bad message of the connection {} , released: {}", connection.getSessionId(),
                                connection.getAttachment());
                        if (r.badMessage != null) {
                            r.badMessage.call(errCode, reason, resp);
                        }
                        if (r.future != null) {
                            if (r.simpleResponse == null) {
                                r.simpleResponse = new SimpleResponse(resp);
                            }
                            r.future.failed(new BadMessageException(errCode, reason));
                        }
                    }).earlyEOF((req, resp, outputStream, conn) -> {
                        release(connection, pool);
                        log.debug("eafly EOF of the connection {} , released: {}", connection.getSessionId(),
                                connection.getAttachment());
                        if (r.earlyEof != null) {
                            r.earlyEof.call(resp);
                        }
                        if (r.future != null) {
                            if (r.simpleResponse == null) {
                                r.simpleResponse = new SimpleResponse(resp);
                            }
                            r.future.failed(new EofException("early eof"));
                        }
                    });

            if (r.requestBody != null && r.requestBody.isEmpty() == false) {
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
            } else {
                connection.send(r.request, handler);
            }
            long end = Millisecond100Clock.currentTimeMillis();
            monitor.info("SimpleHTTPClient take connection total time: {}", (end - start));
        } catch (InterruptedException e) {
            log.error("take connection exception", e);
        }
    }

    private BlockingPool<HTTPClientConnection> getPool(RequestBuilder request) {
        BlockingPool<HTTPClientConnection> pool = poolMap.get(request);
        if (pool == null) {
            synchronized (this) {
                if (pool == null) {
                    SimpleHTTPClientConfiguration config = (SimpleHTTPClientConfiguration) http2Client
                            .getHttp2Configuration();
                    pool = new BoundedBlockingPool<>(config.getInitPoolSize(), config.getMaxPoolSize(), () -> {
                        FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
                        http2Client.connect(request.host, request.port, promise);
                        try {
                            return promise.get();
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("create http connection exception", e);
                            throw new IllegalStateException(e);
                        }
                    }, (conn) -> conn.isOpen(), (conn) -> {
                        try {
                            conn.close();
                        } catch (IOException e) {
                            log.error("close http connection exception", e);
                        }
                    });
                    poolMap.put(request, pool);
                    return pool;
                }
            }
        }
        return pool;
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {
        http2Client.stop();
    }
}
