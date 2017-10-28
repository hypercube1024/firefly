package com.firefly.server.http2;

import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.StringUtils;
import com.firefly.utils.function.Action1;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.lang.GenericTypeReference;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SimpleRequest {

    final Request request;
    final SimpleResponse response;
    final HTTPConnection connection;
    Action1<ByteBuffer> content;
    Action1<SimpleRequest> contentComplete;
    Action1<SimpleRequest> messageComplete;
    List<ByteBuffer> requestBody = new ArrayList<>();

    List<Cookie> cookies;
    String stringBody;

    final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    public SimpleRequest(Request request, Response response,
                         HTTPOutputStream output,
                         HTTPConnection connection) {
        this.request = request;
        response.setStatus(HttpStatus.OK_200);
        response.setHttpVersion(HttpVersion.HTTP_1_1);
        this.response = new SimpleResponse(response, output, request.getURI());
        this.connection = connection;
    }

    public HttpVersion getHttpVersion() {
        return request.getHttpVersion();
    }

    public HttpFields getFields() {
        return request.getFields();
    }

    public long getContentLength() {
        return getFields().getLongField(HttpHeader.CONTENT_LENGTH.asString());
    }

    public Iterator<HttpField> iterator() {
        return request.iterator();
    }

    public String getMethod() {
        return request.getMethod();
    }

    public HttpURI getURI() {
        return request.getURI();
    }

    public String getURIString() {
        return request.getURIString();
    }

    public Supplier<HttpFields> getTrailerSupplier() {
        return request.getTrailerSupplier();
    }

    public void forEach(Consumer<? super HttpField> action) {
        request.forEach(action);
    }

    public Spliterator<HttpField> spliterator() {
        return request.spliterator();
    }

    public Object get(String key) {
        return attributes.get(key);
    }

    public Object put(String key, Object value) {
        return attributes.put(key, value);
    }

    public Object remove(String key) {
        return attributes.remove(key);
    }

    public ConcurrentHashMap<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return request.toString();
    }

    public Request getRequest() {
        return request;
    }

    public SimpleResponse getResponse() {
        return response;
    }

    public SimpleResponse getAsyncResponse() {
        response.setAsynchronous(true);
        return response;
    }

    public HTTPConnection getConnection() {
        return connection;
    }

    public List<ByteBuffer> getRequestBody() {
        return requestBody;
    }

    public SimpleRequest content(Action1<ByteBuffer> content) {
        this.content = content;
        return this;
    }

    public SimpleRequest contentComplete(Action1<SimpleRequest> contentComplete) {
        this.contentComplete = contentComplete;
        return this;
    }

    public SimpleRequest messageComplete(Action1<SimpleRequest> messageComplete) {
        this.messageComplete = messageComplete;
        return this;
    }

    public String getStringBody(String charset) {
        if (stringBody == null) {
            stringBody = BufferUtils.toString(requestBody, charset);
            return stringBody;
        } else {
            return stringBody;
        }
    }

    public String getStringBody() {
        return getStringBody("UTF-8");
    }

    public <T> T getJsonBody(Class<T> clazz) {
        return Json.toObject(getStringBody(), clazz);
    }

    public <T> T getJsonBody(GenericTypeReference<T> typeReference) {
        return Json.toObject(getStringBody(), typeReference);
    }

    public JsonObject getJsonObjectBody() {
        return Json.toJsonObject(getStringBody());
    }

    public JsonArray getJsonArrayBody() {
        return Json.toJsonArray(getStringBody());
    }

    public List<Cookie> getCookies() {
        if (cookies == null) {
            String v = request.getFields().get(HttpHeader.COOKIE);
            if (StringUtils.hasText(v)) {
                cookies = CookieParser.parseCookie(v);
            }
        }
        return cookies;
    }
}
