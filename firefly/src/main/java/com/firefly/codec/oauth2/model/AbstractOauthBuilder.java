package com.firefly.codec.oauth2.model;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.utils.BeanUtils;
import com.firefly.utils.CollectionUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.collection.MultiMap;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.annotation.JsonProperty;
import com.firefly.utils.json.support.PropertyUtils;
import com.firefly.utils.lang.bean.PropertyAccess;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractOauthBuilder<T extends AbstractOauthBuilder, R> {

    protected UrlEncoded urlEncoded = new UrlEncoded();
    protected T builderInstance;
    protected R object;

    public T put(String name, String value) {
        urlEncoded.put(name, value);
        return builderInstance;
    }

    public T putAllValues(Map<String, String> input) {
        urlEncoded.putAllValues(input);
        return builderInstance;
    }

    public T putValues(String name, List<String> values) {
        urlEncoded.putValues(name, values);
        return builderInstance;
    }

    public T putValues(String name, String... values) {
        urlEncoded.putValues(name, values);
        return builderInstance;
    }

    public T add(String name, String value) {
        urlEncoded.add(name, value);
        return builderInstance;
    }

    public T addValues(String name, List<String> values) {
        urlEncoded.addValues(name, values);
        return builderInstance;
    }

    public T addValues(String name, String[] values) {
        urlEncoded.addValues(name, values);
        return builderInstance;
    }

    public T addAllValues(MultiMap<String> map) {
        urlEncoded.addAllValues(map);
        return builderInstance;
    }

    public T removeValue(String name, String value) {
        urlEncoded.removeValue(name, value);
        return builderInstance;
    }

    public T getBuilderInstance() {
        return builderInstance;
    }

    public R getObject() {
        return object;
    }

    public UrlEncoded getUrlEncoded() {
        return urlEncoded;
    }

    public String toJson() {
        return Json.toJson(toMap());
    }

    @SuppressWarnings("unchecked")
    public String toEncodedUrl() {
        UrlEncoded tmp = new UrlEncoded();
        toMap().forEach((key, value) -> {
            if (value instanceof Collection) {
                tmp.put(key, new ArrayList<>((Collection<String>) value));
            } else {
                tmp.put(key, value.toString());
            }
        });
        return tmp.encode(StandardCharsets.UTF_8, true);
    }

    public Map<String, Object> toMap() {
        Class<?> clazz = object.getClass();
        Map<String, Object> map = new HashMap<>();

        urlEncoded.entrySet().stream()
                  .filter(e -> !CollectionUtils.isEmpty(e.getValue()))
                  .forEach(e -> {
                      if (e.getValue().size() > 1) {
                          map.put(e.getKey(), e.getValue());
                      } else {
                          map.put(e.getKey(), e.getValue().get(0));
                      }
                  });

        BeanUtils.getBeanAccess(clazz).entrySet().stream()
                 .filter(e -> !isTransientField(clazz, e))
                 .forEach(e -> {
                     String name = e.getKey();
                     PropertyAccess property = e.getValue();
                     JsonProperty jsonProperty = PropertyUtils.getJsonProperty(name, clazz, property.getSetterMethod(), property.getGetterMethod());
                     String key = Optional.ofNullable(jsonProperty)
                                          .map(JsonProperty::value)
                                          .filter(StringUtils::hasText)
                                          .orElse(name);
                     map.put(key, property.getValue(object));
                 });
        return map;
    }

    protected boolean isTransientField(Class<?> clazz, Map.Entry<String, PropertyAccess> e) {
        return PropertyUtils.isTransientField(e.getKey(), clazz, e.getValue().getSetterMethod(), e.getValue().getGetterMethod());
    }

}
