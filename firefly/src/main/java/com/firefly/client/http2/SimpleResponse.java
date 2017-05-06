package com.firefly.client.http2;

import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SimpleResponse {

    Response response;
    List<ByteBuffer> responseBody = new ArrayList<>();
    List<Cookie> cookies;
    String stringBody;

    public SimpleResponse(Response response) {
        this.response = response;
    }

    public HttpVersion getHttpVersion() {
        return response.getHttpVersion();
    }

    public HttpFields getFields() {
        return response.getFields();
    }

    public long getContentLength() {
        return response.getContentLength();
    }

    public Iterator<HttpField> iterator() {
        return response.iterator();
    }

    public int getStatus() {
        return response.getStatus();
    }

    public String getReason() {
        return response.getReason();
    }

    public Supplier<HttpFields> getTrailerSupplier() {
        return response.getTrailerSupplier();
    }

    public void forEach(Consumer<? super HttpField> action) {
        response.forEach(action);
    }

    public Spliterator<HttpField> spliterator() {
        return response.spliterator();
    }

    public Response getResponse() {
        return response;
    }

    public List<ByteBuffer> getResponseBody() {
        return responseBody;
    }

    public String getStringBody() {
        return getStringBody("UTF-8");
    }

    public String getStringBody(String charset) {
        if (stringBody == null) {
            stringBody = BufferUtils.toString(responseBody, charset);
            return stringBody;
        } else {
            return stringBody;
        }
    }

    public <T> T getJsonBody(Class<T> clazz) {
        return Json.toObject(getStringBody(), clazz);
    }

    public JsonObject getJsonObjectBody() {
        return Json.toJsonObject(getStringBody());
    }

    public JsonArray getJsonArrayBody() {
        return Json.toJsonArray(getStringBody());
    }

    public List<Cookie> getCookies() {
        if (cookies == null) {
            cookies = response.getFields().getValuesList(HttpHeader.SET_COOKIE.asString()).stream()
                              .map(CookieParser::parseSetCookie).collect(Collectors.toList());
            return cookies;
        } else {
            return cookies;
        }
    }
}
