package com.firefly.codec.oauth2.model;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.utils.collection.MultiMap;
import com.firefly.utils.json.Json;

import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractOauthBuilder<T extends AbstractOauthBuilder, R> {

    protected UrlEncoded urlEncoded = new UrlEncoded();
    protected T instance;
    protected R object;

    public T put(String name, String value) {
        urlEncoded.put(name, value);
        return instance;
    }

    public T putAllValues(Map<String, String> input) {
        urlEncoded.putAllValues(input);
        return instance;
    }

    public T putValues(String name, List<String> values) {
        urlEncoded.putValues(name, values);
        return instance;
    }

    public T putValues(String name, String... values) {
        urlEncoded.putValues(name, values);
        return instance;
    }

    public T add(String name, String value) {
        urlEncoded.add(name, value);
        return instance;
    }

    public T addValues(String name, List<String> values) {
        urlEncoded.addValues(name, values);
        return instance;
    }

    public T addValues(String name, String[] values) {
        urlEncoded.addValues(name, values);
        return instance;
    }

    public T addAllValues(MultiMap<String> map) {
        urlEncoded.addAllValues(map);
        return instance;
    }

    public T removeValue(String name, String value) {
        urlEncoded.removeValue(name, value);
        return instance;
    }

    public T getInstance() {
        return instance;
    }

    public R getObject() {
        return object;
    }

    public UrlEncoded getUrlEncoded() {
        return urlEncoded;
    }

    public String toJson() {
        return Json.toJson(object);
    }

    abstract public String toEncodedUrl();
}
