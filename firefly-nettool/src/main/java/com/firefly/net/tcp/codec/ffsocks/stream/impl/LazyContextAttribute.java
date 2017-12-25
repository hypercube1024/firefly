package com.firefly.net.tcp.codec.ffsocks.stream.impl;

import com.firefly.net.tcp.codec.ffsocks.stream.ContextAttribute;
import com.firefly.utils.concurrent.LazyInitProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pengtao Qiu
 */
public class LazyContextAttribute implements ContextAttribute {

    protected LazyInitProperty<ConcurrentMap<String, Object>> attributes = new LazyInitProperty<>();

    @Override
    public Map<String, Object> getAttributes() {
        return attributes.getProperty(ConcurrentHashMap::new);
    }

    @Override
    public void setAttribute(String key, Object value) {
        getAttributes().put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        return getAttributes().get(key);
    }
}
