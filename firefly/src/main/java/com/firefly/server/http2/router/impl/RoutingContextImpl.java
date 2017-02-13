package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.server.http2.router.RouterManager;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.spi.RoutingContextSPI;

import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pengtao Qiu
 */
public class RoutingContextImpl implements RoutingContext {

    private final SimpleRequest request;
    private final NavigableSet<RouterManager.RouterMatchResult> routers;
    private volatile RouterManager.RouterMatchResult current;
    private volatile RoutingContextSPI routingContextSPI;

    public RoutingContextImpl(SimpleRequest request, NavigableSet<RouterManager.RouterMatchResult> routers) {
        this.request = request;
        this.routers = routers;
    }

    @Override
    public Object get(String key) {
        return request.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return request.put(key, value);
    }

    @Override
    public Object remove(String key) {
        return request.remove(key);
    }

    @Override
    public ConcurrentHashMap<String, Object> getAttributes() {
        return request.getAttributes();
    }

    @Override
    public SimpleResponse getResponse() {
        return request.getResponse();
    }

    @Override
    public SimpleResponse getAsyncResponse() {
        return request.getAsyncResponse();
    }

    @Override
    public SimpleRequest getRequest() {
        return request;
    }

    @Override
    public String getRouterParameter(String name) {
        return current.getParameters().get(name);
    }

    @Override
    public String getParameter(String name) {
        if (routingContextSPI == null) {
            return null;
        } else {
            return routingContextSPI.getParameter(name);
        }
    }

    @Override
    public List<String> getParameterValues(String name) {
        if (routingContextSPI == null) {
            return null;
        } else {
            return routingContextSPI.getParameterValues(name);
        }
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        if (routingContextSPI == null) {
            return null;
        } else {
            return routingContextSPI.getParameterMap();
        }
    }

    @Override
    public Collection<Part> getParts() {
        if (routingContextSPI == null) {
            return null;
        } else {
            return routingContextSPI.getParts();
        }
    }

    @Override
    public Part getPart(String name) {
        if (routingContextSPI == null) {
            return null;
        } else {
            return routingContextSPI.getPart(name);
        }
    }

    @Override
    public HttpSession getHttpSession() {
        if (routingContextSPI == null) {
            return null;
        } else {
            return routingContextSPI.getHttpSession();
        }
    }

    @Override
    public void setRoutingContextSPI(RoutingContextSPI routingContextSPI) {
        this.routingContextSPI = routingContextSPI;
    }

    @Override
    public boolean next() {
        current = routers.pollFirst();
        if (current != null) {
            ((RouterImpl) current.getRouter()).getHandler().handle(this);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        request.getResponse().close();
    }
}
