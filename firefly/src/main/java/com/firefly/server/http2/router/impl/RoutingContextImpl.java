package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.server.http2.router.RoutingContext;

import javax.servlet.http.Part;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pengtao Qiu
 */
public class RoutingContextImpl implements RoutingContext {
    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public Object put(String key, Object value) {
        return null;
    }

    @Override
    public Object remove(String key) {
        return null;
    }

    @Override
    public ConcurrentHashMap<String, Object> getAttributes() {
        return null;
    }

    @Override
    public SimpleResponse getResponse() {
        return null;
    }

    @Override
    public SimpleRequest getRequest() {
        return null;
    }

    @Override
    public String getPathParameter(String name) {
        return null;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public List<String> getParameterValues(String name) {
        return null;
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        return null;
    }

    @Override
    public Collection<Part> getParts() {
        return null;
    }

    @Override
    public Part getPart(String name) {
        return null;
    }

    @Override
    public void next() {

    }
}
