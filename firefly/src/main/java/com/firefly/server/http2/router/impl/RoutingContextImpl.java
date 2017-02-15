package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.server.http2.router.RouterManager;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.spi.HTTPBodyHandlerSPI;
import com.firefly.server.http2.router.spi.HTTPSessionHandlerSPI;
import com.firefly.utils.function.Action1;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;

import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
    private volatile HTTPBodyHandlerSPI httpBodyHandlerSPI;
    private volatile HTTPSessionHandlerSPI httpSessionHandlerSPI;
    private volatile boolean asynchronousRead;

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
    public RoutingContext content(Action1<ByteBuffer> content) {
        request.content(content);
        asynchronousRead = true;
        return this;
    }

    @Override
    public RoutingContext contentComplete(Action1<SimpleRequest> contentComplete) {
        request.contentComplete(contentComplete);
        asynchronousRead = true;
        return this;
    }

    @Override
    public RoutingContext messageComplete(Action1<SimpleRequest> messageComplete) {
        request.messageComplete(messageComplete);
        asynchronousRead = true;
        return this;
    }

    @Override
    public boolean isAsynchronousRead() {
        return asynchronousRead;
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
    public boolean hasNext() {
        return !routers.isEmpty();
    }

    @Override
    public void close() throws IOException {
        request.getResponse().close();
    }

    @Override
    public String getParameter(String name) {
        if (httpBodyHandlerSPI == null) {
            return null;
        } else {
            return httpBodyHandlerSPI.getParameter(name);
        }
    }

    @Override
    public List<String> getParameterValues(String name) {
        if (httpBodyHandlerSPI == null) {
            return null;
        } else {
            return httpBodyHandlerSPI.getParameterValues(name);
        }
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        if (httpBodyHandlerSPI == null) {
            return null;
        } else {
            return httpBodyHandlerSPI.getParameterMap();
        }
    }

    @Override
    public Collection<Part> getParts() {
        if (httpBodyHandlerSPI == null) {
            return null;
        } else {
            return httpBodyHandlerSPI.getParts();
        }
    }

    @Override
    public Part getPart(String name) {
        if (httpBodyHandlerSPI == null) {
            return null;
        } else {
            return httpBodyHandlerSPI.getPart(name);
        }
    }

    @Override
    public InputStream getInputStream() {
        if (httpBodyHandlerSPI == null) {
            return null;
        } else {
            return httpBodyHandlerSPI.getInputStream();
        }
    }

    @Override
    public BufferedReader getBufferedReader() {
        if (httpBodyHandlerSPI == null) {
            return null;
        } else {
            return httpBodyHandlerSPI.getBufferedReader();
        }
    }

    @Override
    public String getStringBody(String charset) {
        if (httpBodyHandlerSPI == null) {
            return request.getStringBody(charset);
        } else {
            return httpBodyHandlerSPI.getStringBody(charset);
        }
    }

    @Override
    public String getStringBody() {
        if (httpBodyHandlerSPI == null) {
            return request.getStringBody();
        } else {
            return httpBodyHandlerSPI.getStringBody();
        }
    }

    @Override
    public <T> T getJsonBody(Class<T> clazz) {
        if (httpBodyHandlerSPI == null) {
            return request.getJsonBody(clazz);
        } else {
            return httpBodyHandlerSPI.getJsonBody(clazz);
        }
    }

    @Override
    public JsonObject getJsonObjectBody() {
        if (httpBodyHandlerSPI == null) {
            return request.getJsonObjectBody();
        } else {
            return httpBodyHandlerSPI.getJsonObjectBody();
        }
    }

    @Override
    public JsonArray getJsonArrayBody() {
        if (httpBodyHandlerSPI == null) {
            return request.getJsonArrayBody();
        } else {
            return httpBodyHandlerSPI.getJsonArrayBody();
        }
    }

    @Override
    public void setHTTPBodyHandlerSPI(HTTPBodyHandlerSPI httpBodyHandlerSPI) {
        this.httpBodyHandlerSPI = httpBodyHandlerSPI;
    }


    @Override
    public HttpSession getSession() {
        if (httpSessionHandlerSPI == null) {
            return null;
        } else {
            return httpSessionHandlerSPI.getSession();
        }
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (httpSessionHandlerSPI == null) {
            return null;
        } else {
            return httpSessionHandlerSPI.getSession(create);
        }
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return httpSessionHandlerSPI != null && httpSessionHandlerSPI.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return httpSessionHandlerSPI != null && httpSessionHandlerSPI.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return httpSessionHandlerSPI != null && httpSessionHandlerSPI.isRequestedSessionIdValid();
    }

    @Override
    public String getRequestedSessionId() {
        if (httpSessionHandlerSPI == null) {
            return null;
        } else {
            return httpSessionHandlerSPI.getRequestedSessionId();
        }
    }

    @Override
    public void setHTTPSessionHandlerSPI(HTTPSessionHandlerSPI httpSessionHandlerSPI) {
        this.httpSessionHandlerSPI = httpSessionHandlerSPI;
    }
}
