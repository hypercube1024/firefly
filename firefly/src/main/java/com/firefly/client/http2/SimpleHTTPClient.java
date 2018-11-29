package com.firefly.client.http2;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.Timer;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.oauth2.model.*;
import com.firefly.utils.CollectionUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action3;
import com.firefly.utils.function.Func1;
import com.firefly.utils.heartbeat.HealthCheck;
import com.firefly.utils.heartbeat.Task;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.EofException;
import com.firefly.utils.io.IO;
import com.firefly.utils.json.Json;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.pool.PooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SimpleHTTPClient extends AbstractLifeCycle {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    private final HTTP2Client http2Client;
    private final ConcurrentHashMap<RequestBuilder, HttpClientConnectionManager> poolMap = new ConcurrentHashMap<>();
    private final SimpleHTTPClientConfiguration config;
    private final Timer responseTimer;
    private final Meter errorMeter;

    public SimpleHTTPClient() {
        this(new SimpleHTTPClientConfiguration());
    }

    public SimpleHTTPClient(SimpleHTTPClientConfiguration http2Configuration) {
        this.config = http2Configuration;
        http2Client = new HTTP2Client(http2Configuration);
        MetricRegistry metrics = http2Configuration.getTcpConfiguration().getMetricReporterFactory().getMetricRegistry();
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

    /**
     * The HTTP request builder that helps you to create a new HTTP request.
     */
    public class RequestBuilder {
        protected String host;
        protected int port;
        protected MetaData.Request request;

        List<ByteBuffer> requestBody = new ArrayList<>();

        Func1<HTTPClientConnection, CompletableFuture<Boolean>> connect;
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
        UrlEncoded queryUrlEncoded;

        SettingsFrame settingsFrame;

        Promise.Completable<SimpleResponse> future;
        SimpleResponse simpleResponse;

        protected RequestBuilder() {

        }

        protected RequestBuilder(String host, int port, MetaData.Request request) {
            this.host = host;
            this.port = port;
            this.request = request;
        }

        /**
         * Set the cookies.
         *
         * @param cookies The cookies.
         * @return RequestBuilder
         */
        public RequestBuilder cookies(List<Cookie> cookies) {
            request.getFields().put(HttpHeader.COOKIE, CookieGenerator.generateCookies(cookies));
            return this;
        }

        /**
         * Put an HTTP field. It will replace existed field.
         *
         * @param name The field name.
         * @param list The field values.
         * @return RequestBuilder
         */
        public RequestBuilder put(String name, List<String> list) {
            request.getFields().put(name, list);
            return this;
        }

        /**
         * Put an HTTP field. It will replace existed field.
         *
         * @param header The field name.
         * @param value  The field value.
         * @return RequestBuilder
         */
        public RequestBuilder put(HttpHeader header, String value) {
            request.getFields().put(header, value);
            return this;
        }

        /**
         * Put an HTTP field. It will replace existed field.
         *
         * @param name  The field name.
         * @param value The field value.
         * @return RequestBuilder
         */
        public RequestBuilder put(String name, String value) {
            request.getFields().put(name, value);
            return this;
        }

        /**
         * Put an HTTP field. It will replace existed field.
         *
         * @param field The HTTP field.
         * @return RequestBuilder
         */
        public RequestBuilder put(HttpField field) {
            request.getFields().put(field);
            return this;
        }

        /**
         * Add some HTTP fields.
         *
         * @param fields The HTTP fields.
         * @return RequestBuilder
         */
        public RequestBuilder addAll(HttpFields fields) {
            request.getFields().addAll(fields);
            return this;
        }

        /**
         * Add an HTTP field.
         *
         * @param field The HTTP field.
         * @return RequestBuilder
         */
        public RequestBuilder add(HttpField field) {
            request.getFields().add(field);
            return this;
        }

        /**
         * Get the HTTP trailers.
         *
         * @return The HTTP trailers.
         */
        public Supplier<HttpFields> getTrailerSupplier() {
            return request.getTrailerSupplier();
        }

        /**
         * Set the HTTP trailers.
         *
         * @param trailers The HTTP trailers.
         * @return RequestBuilder
         */
        public RequestBuilder setTrailerSupplier(Supplier<HttpFields> trailers) {
            request.setTrailerSupplier(trailers);
            return this;
        }

        /**
         * Set the JSON HTTP body data.
         *
         * @param obj The JSON HTTP body data. The HTTP client will serialize the object when the request is submitted.
         * @return RequestBuilder
         */
        public RequestBuilder jsonBody(Object obj) {
            return put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.asString()).body(Json.toJson(obj));
        }

        /**
         * Set the text HTTP body data.
         *
         * @param content The text HTTP body data.
         * @return RequestBuilder
         */
        public RequestBuilder body(String content) {
            return body(content, StandardCharsets.UTF_8);
        }

        /**
         * Set the text HTTP body data.
         *
         * @param content The text HTTP body data.
         * @param charset THe charset of the text.
         * @return RequestBuilder
         */
        public RequestBuilder body(String content, Charset charset) {
            return write(BufferUtils.toBuffer(content, charset));
        }

        /**
         * Write HTTP body data. When you submit the request, the data will be sent.
         *
         * @param buffer The HTTP body data.
         * @return RequestBuilder
         */
        public RequestBuilder write(ByteBuffer buffer) {
            requestBody.add(buffer);
            return this;
        }

        /**
         * Set a output stream callback. When the HTTP client creates the HTTPOutputStream, it will execute this callback.
         *
         * @param output The output stream callback.
         * @return RequestBuilder
         */
        public RequestBuilder output(Action1<HTTPOutputStream> output) {
            this.output = output;
            return this;
        }

        /**
         * Set a output stream callback. When the HTTP client creates the HTTPOutputStream, it will execute this callback.
         *
         * @param promise The output stream callback.
         * @return RequestBuilder
         */
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

        /**
         * Add a multi-part mime content. Such as a file.
         *
         * @param name    The content name.
         * @param content The ContentProvider that helps you read the content.
         * @param fields  The header fields of the content.
         * @return RequestBuilder
         */
        public RequestBuilder addFieldPart(String name, ContentProvider content, HttpFields fields) {
            multiPartProvider().addFieldPart(name, content, fields);
            return this;
        }

        /**
         * Add a multi-part mime content. Such as a file.
         *
         * @param name     The content name.
         * @param fileName The content file name.
         * @param content  The ContentProvider that helps you read the content.
         * @param fields   The header fields of the content.
         * @return RequestBuilder
         */
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

        UrlEncoded queryUrlEncoded() {
            if (queryUrlEncoded == null) {
                queryUrlEncoded = new UrlEncoded();
            }
            return queryUrlEncoded;
        }

        /**
         * Add a value in an existed form parameter. The form content type is "application/x-www-form-urlencoded".
         *
         * @param name  The parameter name.
         * @param value The parameter value.
         * @return RequestBuilder
         */
        public RequestBuilder addFormParam(String name, String value) {
            formUrlEncoded().add(name, value);
            return this;
        }

        /**
         * Add some values in an existed form parameter. The form content type is "application/x-www-form-urlencoded".
         *
         * @param name   The parameter name.
         * @param values The parameter values.
         * @return RequestBuilder
         */
        public RequestBuilder addFormParam(String name, List<String> values) {
            formUrlEncoded().addValues(name, values);
            return this;
        }

        /**
         * Put a parameter in the form content. The form content type is "application/x-www-form-urlencoded".
         *
         * @param name  The parameter name.
         * @param value The parameter value.
         * @return RequestBuilder
         */
        public RequestBuilder putFormParam(String name, String value) {
            formUrlEncoded().put(name, value);
            return this;
        }

        /**
         * Put a parameter in the form content. The form content type is "application/x-www-form-urlencoded".
         *
         * @param name   The parameter name.
         * @param values The parameter values.
         * @return RequestBuilder
         */
        public RequestBuilder putFormParam(String name, List<String> values) {
            formUrlEncoded().putValues(name, values);
            return this;
        }

        /**
         * Remove a parameter in the form content. The form content type is "application/x-www-form-urlencoded".
         *
         * @param name The parameter name.
         * @return RequestBuilder
         */
        public RequestBuilder removeFormParam(String name) {
            formUrlEncoded().remove(name);
            return this;
        }

        /**
         * Add a value in an existed query parameter.
         *
         * @param name  The parameter name.
         * @param value The parameter value.
         * @return RequestBuilder
         */
        public RequestBuilder addQueryParam(String name, String value) {
            queryUrlEncoded().add(name, value);
            return this;
        }

        /**
         * Add some values in an existed query parameter.
         *
         * @param name   The parameter name.
         * @param values The parameter values.
         * @return RequestBuilder
         */
        public RequestBuilder addQueryParam(String name, List<String> values) {
            queryUrlEncoded().addValues(name, values);
            return this;
        }

        /**
         * Put a parameter in the query parameter.
         *
         * @param name  The parameter name.
         * @param value The parameter value.
         * @return RequestBuilder
         */
        public RequestBuilder putQueryParam(String name, String value) {
            queryUrlEncoded().put(name, value);
            return this;
        }

        /**
         * Put a parameter in the query parameter.
         *
         * @param name   The parameter name.
         * @param values The parameter values.
         * @return RequestBuilder
         */
        public RequestBuilder putQueryParam(String name, List<String> values) {
            queryUrlEncoded().putValues(name, values);
            return this;
        }

        /**
         * Remove a parameter in the query parameter.
         *
         * @param name The parameter name.
         * @return RequestBuilder
         */
        public RequestBuilder removeQueryParam(String name) {
            queryUrlEncoded().remove(name);
            return this;
        }

        /**
         * Set the connection establishing callback.
         *
         * @param connect the connection establishing callback
         * @return RequestBuilder
         */
        public RequestBuilder connect(Func1<HTTPClientConnection, CompletableFuture<Boolean>> connect) {
            this.connect = connect;
            return this;
        }

        /**
         * Set the HTTP header complete callback.
         *
         * @param headerComplete The HTTP header complete callback. When the HTTP client receives all HTTP headers,
         *                       it will execute this action.
         * @return RequestBuilder
         */
        public RequestBuilder headerComplete(Action1<Response> headerComplete) {
            this.headerComplete = headerComplete;
            return this;
        }

        /**
         * Set the HTTP message complete callback.
         *
         * @param messageComplete The HTTP message complete callback. When the HTTP client receives the complete HTTP message
         *                        that contains HTTP headers and body, it will execute this action.
         * @return RequestBuilder
         */
        public RequestBuilder messageComplete(Action1<Response> messageComplete) {
            this.messageComplete = messageComplete;
            return this;
        }

        /**
         * Set the HTTP content receiving callback.
         *
         * @param content The HTTP content receiving callback. When the HTTP client receives the HTTP body data,
         *                it will execute this action. This action will be executed many times.
         * @return RequestBuilder
         */
        public RequestBuilder content(Action1<ByteBuffer> content) {
            this.content = content;
            return this;
        }

        /**
         * Set the HTTP content complete callback.
         *
         * @param contentComplete The HTTP content complete callback. When the HTTP client receives the HTTP body finish,
         *                        it will execute this action.
         * @return RequestBuilder
         */
        public RequestBuilder contentComplete(Action1<Response> contentComplete) {
            this.contentComplete = contentComplete;
            return this;
        }

        /**
         * Set the bad message callback.
         *
         * @param badMessage The bad message callback. When the HTTP client parses an incorrect message format,
         *                   it will execute this action. The callback has three parameters.
         *                   The first parameter is the bad status code.
         *                   The second parameter is the reason.
         *                   The third parameter is HTTP response.
         * @return RequestBuilder
         */
        public RequestBuilder badMessage(Action3<Integer, String, Response> badMessage) {
            this.badMessage = badMessage;
            return this;
        }

        /**
         * Set the early EOF callback.
         *
         * @param earlyEof The early EOF callback. When the HTTP client encounters an error, it will execute this action.
         * @return RequestBuilder
         */
        public RequestBuilder earlyEof(Action1<Response> earlyEof) {
            this.earlyEof = earlyEof;
            return this;
        }

        /**
         * send an HTTP2 settings frame
         *
         * @param settingsFrame The HTTP2 settings frame
         * @return RequestBuilder
         */
        public RequestBuilder settings(SettingsFrame settingsFrame) {
            this.settingsFrame = settingsFrame;
            return this;
        }

        /**
         * Build a OAuth2 authorization request
         *
         * @param authRequest The OAuth2 authorization request
         * @return RequestBuilder
         */
        public RequestBuilder authRequest(AuthorizationRequest.Builder authRequest) {
            buildAccessTokenRequest(authRequest.toMap());
            return this;
        }

        /**
         * Build code access token request
         *
         * @param codeAccessTokenRequest The code access token request
         * @return RequestBuilder
         */
        public RequestBuilder codeAccessTokenRequest(AuthorizationCodeAccessTokenRequest.Builder codeAccessTokenRequest) {
            buildAccessTokenRequest(codeAccessTokenRequest.toMap());
            return this;
        }

        /**
         * Build username and password access token request
         *
         * @param pwdAccessTokenRequest The username and password access token request
         * @return RequestBuilder
         */
        public RequestBuilder pwdAccessTokenRequest(PasswordAccessTokenRequest.Builder pwdAccessTokenRequest) {
            buildAccessTokenRequest(pwdAccessTokenRequest.toMap());
            return this;
        }

        /**
         * Build credential access token request
         *
         * @param credAccessTokenRequest The credential access token request
         * @return RequestBuilder
         */
        public RequestBuilder credAccessTokenRequest(ClientCredentialAccessTokenRequest.Builder credAccessTokenRequest) {
            buildAccessTokenRequest(credAccessTokenRequest.toMap());
            return this;
        }

        /**
         * Build refreshing token request
         *
         * @param refreshTokenRequest The refreshing token request
         * @return RequestBuilder
         */
        public RequestBuilder refreshTokenRequest(RefreshingTokenRequest.Builder refreshTokenRequest) {
            buildAccessTokenRequest(refreshTokenRequest.toMap());
            return this;
        }

        @SuppressWarnings("unchecked")
        protected void buildAccessTokenRequest(Map<String, Object> paramMap) {
            UrlEncoded p;
            switch (HttpMethod.fromString(request.getMethod())) {
                case GET:
                    p = queryUrlEncoded();
                    break;
                case POST:
                    p = formUrlEncoded();
                    break;
                default:
                    p = null;
            }
            if (p != null) {
                paramMap.forEach((key, value) -> {
                    if (value instanceof Collection) {
                        p.put(key, new ArrayList<>((Collection<String>) value));
                    } else {
                        p.put(key, value.toString());
                    }
                });
            }
        }

        /**
         * Submit an HTTP request.
         *
         * @return The CompletableFuture of HTTP response.
         */
        public Promise.Completable<SimpleResponse> submit() {
            submit(new Promise.Completable<>());
            return future;
        }

        /**
         * Submit an HTTP request.
         *
         * @return The CompletableFuture of HTTP response.
         */
        public CompletableFuture<SimpleResponse> toFuture() {
            return submit();
        }

        /**
         * Submit an HTTP request.
         *
         * @param future The HTTP response callback.
         */
        public void submit(Promise.Completable<SimpleResponse> future) {
            this.future = future;
            send(this);
        }

        /**
         * Submit an HTTP request.
         *
         * @param action The HTTP response callback.
         */
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

        /**
         * Submit an HTTP request.
         */
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

    /**
     * Remove the HTTP connection pool.
     *
     * @param url The host URL.
     */
    public void removeConnectionPool(String url) {
        try {
            removeConnectionPool(new URL(url));
        } catch (MalformedURLException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Remove the HTTP connection pool.
     *
     * @param url The host URL.
     */
    public void removeConnectionPool(URL url) {
        RequestBuilder req = new RequestBuilder();
        req.host = url.getHost();
        req.port = url.getPort() < 0 ? url.getDefaultPort() : url.getPort();
        removePool(req);
    }

    /**
     * Remove the HTTP connection pool.
     *
     * @param host The host URL.
     * @param port The target port.
     */
    public void removeConnectionPool(String host, int port) {
        RequestBuilder req = new RequestBuilder();
        req.host = host;
        req.port = port;
        removePool(req);
    }

    private void removePool(RequestBuilder req) {
        Optional.ofNullable(poolMap.remove(req)).ifPresent(HttpClientConnectionManager::stop);
    }

    /**
     * Get the HTTP connection pool size.
     *
     * @param host The host name.
     * @param port The target port.
     * @return The HTTP connection pool size.
     */
    public int getConnectionPoolSize(String host, int port) {
        RequestBuilder req = new RequestBuilder();
        req.host = host;
        req.port = port;
        return _getPoolSize(req);
    }

    /**
     * Get the HTTP connection pool size.
     *
     * @param url The host URL.
     * @return The HTTP connection pool size.
     */
    public int getConnectionPoolSize(String url) {
        try {
            return getConnectionPoolSize(new URL(url));
        } catch (MalformedURLException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Get the HTTP connection pool size.
     *
     * @param url The host URL.
     * @return The HTTP connection pool size.
     */
    public int getConnectionPoolSize(URL url) {
        RequestBuilder req = new RequestBuilder();
        req.host = url.getHost();
        req.port = url.getPort() < 0 ? url.getDefaultPort() : url.getPort();
        return _getPoolSize(req);
    }

    private int _getPoolSize(RequestBuilder req) {
        return Optional.ofNullable(poolMap.get(req)).map(HttpClientConnectionManager::size).orElse(0);
    }

    /**
     * Create a RequestBuilder with GET method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    public RequestBuilder get(String url) {
        return request(HttpMethod.GET.asString(), url);
    }

    /**
     * Create a RequestBuilder with POST method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    public RequestBuilder post(String url) {
        return request(HttpMethod.POST.asString(), url);
    }

    /**
     * Create a RequestBuilder with HEAD method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    public RequestBuilder head(String url) {
        return request(HttpMethod.HEAD.asString(), url);
    }

    /**
     * Create a RequestBuilder with PUT method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    public RequestBuilder put(String url) {
        return request(HttpMethod.PUT.asString(), url);
    }

    /**
     * Create a RequestBuilder with DELETE method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    public RequestBuilder delete(String url) {
        return request(HttpMethod.DELETE.asString(), url);
    }

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method HTTP method.
     * @param url    The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    public RequestBuilder request(HttpMethod method, String url) {
        return request(method.asString(), url);
    }

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method HTTP method.
     * @param url    The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    public RequestBuilder request(String method, String url) {
        try {
            return request(method, new URL(url));
        } catch (MalformedURLException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method HTTP method.
     * @param url    The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    public RequestBuilder request(String method, URL url) {
        try {
            RequestBuilder req = new RequestBuilder();
            req.host = url.getHost();
            req.port = url.getPort() < 0 ? url.getDefaultPort() : url.getPort();
            HttpURI httpURI = new HttpURI(url.toURI());
            if (!StringUtils.hasText(httpURI.getPath().trim())) {
                httpURI.setPath("/");
            }
            req.request = new MetaData.Request(method, httpURI, HttpVersion.HTTP_1_1, new HttpFields());
            return req;
        } catch (URISyntaxException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Register an health check task.
     *
     * @param task The health check task.
     */
    public void registerHealthCheck(Task task) {
        Optional.ofNullable(config.getHealthCheck())
                .ifPresent(healthCheck -> healthCheck.register(task));
    }

    /**
     * Clear the health check task.
     *
     * @param name The task name.
     */
    public void clearHealthCheck(String name) {
        Optional.ofNullable(config.getHealthCheck())
                .ifPresent(healthCheck -> healthCheck.clear(name));
    }

    protected void send(RequestBuilder reqBuilder) {
        Timer.Context resTimerCtx = responseTimer.time();
        getPool(reqBuilder).asyncGet().thenAccept(pooledConn -> {
            HTTPClientConnection connection = pooledConn.getObject();

            if (connection.getHttpVersion() == HttpVersion.HTTP_2) {
                if (reqBuilder.settingsFrame != null) {
                    HTTP2ClientConnection http2ClientConnection = (HTTP2ClientConnection) connection;
                    http2ClientConnection.getHttp2Session().settings(reqBuilder.settingsFrame, Callback.NOOP);
                }
                pooledConn.release();
            }
            if (log.isDebugEnabled()) {
                log.debug("take the connection {} from pool, released: {}, {}", connection.getSessionId(), pooledConn.isReleased(), connection.getHttpVersion());
            }

            if (reqBuilder.connect != null) {
                reqBuilder.connect.call(connection).thenAccept(isSendReq -> {
                    if (isSendReq) {
                        send(reqBuilder, resTimerCtx, connection, createClientHTTPHandler(reqBuilder, resTimerCtx, pooledConn));
                    } else {
                        IO.close(connection);
                    }
                }).exceptionally(ex -> {
                    IO.close(connection);
                    return null;
                });
            } else {
                send(reqBuilder, resTimerCtx, connection, createClientHTTPHandler(reqBuilder, resTimerCtx, pooledConn));
            }
        }).exceptionally(e -> {
            log.error("SimpleHTTPClient sends message exception", e);
            resTimerCtx.stop();
            errorMeter.mark();
            return null;
        });
    }

    protected void send(RequestBuilder reqBuilder, Timer.Context resTimerCtx, HTTPClientConnection connection, ClientHTTPHandler handler) {
        if (reqBuilder.queryUrlEncoded != null) {
            String query = reqBuilder.request.getURI().getQuery();
            if (StringUtils.hasText(query)) {
                reqBuilder.request.getURI().setQuery(query + "&" + reqBuilder.queryUrlEncoded.encode(StandardCharsets.UTF_8, true));
            } else {
                reqBuilder.request.getURI().setQuery(reqBuilder.queryUrlEncoded.encode(StandardCharsets.UTF_8, true));
            }
        }

        if (!CollectionUtils.isEmpty(reqBuilder.requestBody)) {
            connection.send(reqBuilder.request, reqBuilder.requestBody.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), handler);
        } else if (reqBuilder.promise != null) {
            connection.send(reqBuilder.request, reqBuilder.promise, handler);
        } else if (reqBuilder.output != null) {
            Promise<HTTPOutputStream> p = new Promise<HTTPOutputStream>() {
                public void succeeded(HTTPOutputStream out) {
                    reqBuilder.output.call(out);
                }
            };
            connection.send(reqBuilder.request, p, handler);
        } else if (reqBuilder.multiPartProvider != null) {
            IO.close(reqBuilder.multiPartProvider);
            reqBuilder.multiPartProvider.setListener(() -> log.debug("multi part content listener"));
            if (reqBuilder.multiPartProvider.getLength() > 0) {
                reqBuilder.put(HttpHeader.CONTENT_LENGTH, String.valueOf(reqBuilder.multiPartProvider.getLength()));
            }
            Promise.Completable<HTTPOutputStream> p = new Promise.Completable<>();
            connection.send(reqBuilder.request, p, handler);
            p.thenAccept(output -> {
                try (HTTPOutputStream out = output) {
                    for (ByteBuffer buf : reqBuilder.multiPartProvider) {
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
        } else if (reqBuilder.formUrlEncoded != null) {
            String body = reqBuilder.formUrlEncoded.encode(Charset.forName(config.getCharacterEncoding()), true);
            byte[] content = StringUtils.getBytes(body, config.getCharacterEncoding());
            connection.send(reqBuilder.request, ByteBuffer.wrap(content), handler);
        } else {
            connection.send(reqBuilder.request, handler);
        }
    }

    protected ClientHTTPHandler createClientHTTPHandler(RequestBuilder reqBuilder,
                                                        Timer.Context resTimerCtx,
                                                        PooledObject<HTTPClientConnection> pooledConn) {
        return new ClientHTTPHandler.Adapter().headerComplete((req, resp, outputStream, conn) -> {
            Optional.ofNullable(reqBuilder.headerComplete).ifPresent(header -> header.call(resp));
            if (reqBuilder.future != null) {
                if (reqBuilder.simpleResponse == null) {
                    reqBuilder.simpleResponse = new SimpleResponse(resp);
                }
            }
            return HttpMethod.HEAD.is(req.getMethod()) && messageComplete(reqBuilder, resTimerCtx, pooledConn, resp);
        }).content((buffer, req, resp, outputStream, conn) -> {
            Optional.ofNullable(reqBuilder.content).ifPresent(c -> c.call(buffer));
            if (reqBuilder.future != null) {
                Optional.ofNullable(reqBuilder.simpleResponse).map(r -> r.responseBody).ifPresent(body -> body.add(buffer));
            }
            return false;
        }).contentComplete((req, resp, outputStream, conn) -> {
            Optional.ofNullable(reqBuilder.contentComplete).ifPresent(content -> content.call(resp));
            return false;
        }).badMessage((errCode, reason, req, resp, outputStream, conn) -> {
            try {
                Optional.ofNullable(reqBuilder.badMessage).ifPresent(bad -> bad.call(errCode, reason, resp));
                if (reqBuilder.future != null) {
                    if (reqBuilder.simpleResponse == null) {
                        reqBuilder.simpleResponse = new SimpleResponse(resp);
                    }
                    reqBuilder.future.failed(new BadMessageException(errCode, reason));
                }
            } finally {
                errorMeter.mark();
                resTimerCtx.stop();
                IO.close(pooledConn.getObject());
                pooledConn.release();
                if (log.isDebugEnabled()) {
                    log.debug("bad message of the connection {}, released: {}", pooledConn.getObject().getSessionId(), pooledConn.isReleased());
                }
            }
        }).earlyEOF((req, resp, outputStream, conn) -> {
            try {
                Optional.ofNullable(reqBuilder.earlyEof).ifPresent(e -> e.call(resp));
                if (reqBuilder.future != null) {
                    if (reqBuilder.simpleResponse == null) {
                        reqBuilder.simpleResponse = new SimpleResponse(resp);
                    }
                    reqBuilder.future.failed(new EofException("early eof"));
                }
            } finally {
                errorMeter.mark();
                resTimerCtx.stop();
                IO.close(pooledConn.getObject());
                pooledConn.release();
                if (log.isDebugEnabled()) {
                    log.debug("early EOF of the connection {}, released: {}", pooledConn.getObject().getSessionId(), pooledConn.isReleased());
                }
            }
        }).messageComplete((req, resp, outputStream, conn) -> messageComplete(reqBuilder, resTimerCtx, pooledConn, resp));
    }

    private boolean messageComplete(RequestBuilder reqBuilder,
                                    Timer.Context resTimerCtx,
                                    PooledObject<HTTPClientConnection> pooledConn,
                                    Response resp) {
        try {
            Optional.ofNullable(reqBuilder.messageComplete).ifPresent(msg -> msg.call(resp));
            Optional.ofNullable(reqBuilder.future).ifPresent(f -> f.succeeded(reqBuilder.simpleResponse));
            return true;
        } finally {
            resTimerCtx.stop();
            pooledConn.release();
            if (log.isDebugEnabled()) {
                log.debug("complete request of the connection {} , released: {}", pooledConn.getObject().getSessionId(), pooledConn.isReleased());
            }
        }
    }

    protected HttpClientConnectionManager getPool(RequestBuilder request) {
        return poolMap.computeIfAbsent(request, this::createConnectionManager);
    }

    protected HttpClientConnectionManager createConnectionManager(RequestBuilder request) {
        return new HttpClientConnectionManager(http2Client, request.host, request.port,
                config.getPoolSize(), config.getConnectTimeout(),
                config.getLeakDetectorInterval(),
                config.getMaxGettingThreadNum(), config.getMaxReleaseThreadNum());
    }

    @Override
    protected void init() {
        Optional.ofNullable(config.getHealthCheck()).ifPresent(HealthCheck::start);
    }

    @Override
    protected void destroy() {
        http2Client.stop();
        poolMap.forEach((k, v) -> v.stop());
        Optional.ofNullable(config.getHealthCheck()).ifPresent(HealthCheck::stop);
    }
}
