package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.server.http2.router.RouterManager;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.handler.template.TemplateHandlerSPILoader;
import com.firefly.server.http2.router.spi.AsynchronousHttpSession;
import com.firefly.server.http2.router.spi.HTTPBodyHandlerSPI;
import com.firefly.server.http2.router.spi.HTTPSessionHandlerSPI;
import com.firefly.server.http2.router.spi.TemplateHandlerSPI;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action1;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.lang.GenericTypeReference;

import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Pengtao Qiu
 */
public class RoutingContextImpl implements RoutingContext {

    private final SimpleRequest request;
    private final NavigableSet<RouterManager.RouterMatchResult> routers;
    private volatile RouterManager.RouterMatchResult current;
    private volatile HTTPBodyHandlerSPI httpBodyHandlerSPI;
    private volatile HTTPSessionHandlerSPI httpSessionHandlerSPI;
    private final TemplateHandlerSPI templateHandlerSPI = TemplateHandlerSPILoader.getInstance().getTemplateHandlerSPI();
    private volatile boolean asynchronousRead;
    private volatile ConcurrentLinkedDeque<Promise<?>> handlerPromiseQueue;

    RoutingContextImpl(SimpleRequest request, NavigableSet<RouterManager.RouterMatchResult> routers) {
        this.request = request;
        this.routers = routers;
    }

    @Override
    public Object getAttribute(String key) {
        return request.get(key);
    }

    @Override
    public Object setAttribute(String key, Object value) {
        return request.put(key, value);
    }

    @Override
    public Object removeAttribute(String key) {
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
        return Optional.ofNullable(current)
                       .map(RouterManager.RouterMatchResult::getRouter)
                       .map(c -> (RouterImpl) c)
                       .map(RouterImpl::getHandler)
                       .map(handler -> {
                           handler.handle(this);
                           return true;
                       })
                       .orElse(false);
    }

    @Override
    public boolean hasNext() {
        return !routers.isEmpty();
    }

    @Override
    public <T> RoutingContext complete(Promise<T> promise) {
        ConcurrentLinkedDeque<Promise<T>> queue = createHandlerPromiseQueueIfAbsent();
        queue.push(promise);
        return this;
    }

    @Override
    public <T> boolean next(Promise<T> promise) {
        return complete(promise).next();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void succeed(T t) {
        Optional.ofNullable(handlerPromiseQueue)
                .map(ConcurrentLinkedDeque::pop)
                .map(p -> (Promise<T>) p)
                .ifPresent(p -> p.succeeded(t));
    }

    @Override
    public void fail(Throwable x) {
        Optional.ofNullable(handlerPromiseQueue)
                .map(ConcurrentLinkedDeque::pop)
                .ifPresent(p -> p.failed(x));
    }

    @SuppressWarnings("unchecked")
    private <T> ConcurrentLinkedDeque<Promise<T>> createHandlerPromiseQueueIfAbsent() {
        if (handlerPromiseQueue == null) {
            handlerPromiseQueue = new ConcurrentLinkedDeque<>();
        }
        return (ConcurrentLinkedDeque) handlerPromiseQueue;
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
    public <T> T getJsonBody(GenericTypeReference<T> typeReference) {
        if (httpBodyHandlerSPI == null) {
            return request.getJsonBody(typeReference);
        } else {
            return httpBodyHandlerSPI.getJsonBody(typeReference);
        }

    }

    @Override
    public JsonObject getJsonObjectBody() {
        return Optional.ofNullable(httpBodyHandlerSPI)
                       .map(HTTPBodyHandlerSPI::getJsonObjectBody)
                       .orElse(request.getJsonObjectBody());
    }

    @Override
    public JsonArray getJsonArrayBody() {
        return Optional.ofNullable(httpBodyHandlerSPI)
                       .map(HTTPBodyHandlerSPI::getJsonArrayBody)
                       .orElse(request.getJsonArrayBody());
    }

    public void setHTTPBodyHandlerSPI(HTTPBodyHandlerSPI httpBodyHandlerSPI) {
        this.httpBodyHandlerSPI = httpBodyHandlerSPI;
    }


    @Override
    public HttpSession getSession() {
        return Optional.ofNullable(httpSessionHandlerSPI)
                       .map(HTTPSessionHandlerSPI::getSession)
                       .orElse(null);
    }

    @Override
    public HttpSession getSession(boolean create) {
        return Optional.ofNullable(httpSessionHandlerSPI)
                       .map(s -> s.getSession(create))
                       .orElse(null);
    }

    @Override
    public CompletableFuture<AsynchronousHttpSession> getAsyncSession() {
        return Optional.ofNullable(httpSessionHandlerSPI)
                       .map(HTTPSessionHandlerSPI::getAsyncSession)
                       .orElse(null);
    }

    @Override
    public CompletableFuture<AsynchronousHttpSession> getAsyncSession(boolean create) {
        return Optional.ofNullable(httpSessionHandlerSPI)
                       .map(s -> s.getAsyncSession(create))
                       .orElse(null);
    }

    @Override
    public CompletableFuture<Integer> getAsyncSessionSize() {
        return Optional.ofNullable(httpSessionHandlerSPI)
                       .map(HTTPSessionHandlerSPI::getAsyncSessionSize)
                       .orElse(null);
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return Optional.ofNullable(httpSessionHandlerSPI)
                       .map(HTTPSessionHandlerSPI::isRequestedSessionIdFromURL)
                       .orElse(false);
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return Optional.ofNullable(httpSessionHandlerSPI)
                       .map(HTTPSessionHandlerSPI::isRequestedSessionIdFromCookie)
                       .orElse(false);
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return Optional.ofNullable(httpSessionHandlerSPI)
                       .map(HTTPSessionHandlerSPI::isRequestedSessionIdValid)
                       .orElse(false);
    }

    @Override
    public String getRequestedSessionId() {
        return Optional.ofNullable(httpSessionHandlerSPI)
                       .map(HTTPSessionHandlerSPI::getRequestedSessionId)
                       .orElse(null);
    }

    public void setHTTPSessionHandlerSPI(HTTPSessionHandlerSPI httpSessionHandlerSPI) {
        this.httpSessionHandlerSPI = httpSessionHandlerSPI;
    }

    @Override
    public void renderTemplate(String resourceName, Object scope) {
        templateHandlerSPI.renderTemplate(this, resourceName, scope);
    }

    @Override
    public void renderTemplate(String resourceName, Object[] scopes) {
        templateHandlerSPI.renderTemplate(this, resourceName, scopes);
    }

    @Override
    public void renderTemplate(String resourceName, List<Object> scopes) {
        templateHandlerSPI.renderTemplate(this, resourceName, scopes);
    }
}
