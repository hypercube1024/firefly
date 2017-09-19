package com.firefly.server.http2.router;

import com.firefly.codec.http2.model.*;
import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action1;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.lang.GenericTypeReference;

import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A new RoutingContext(ctx) instance is created for each HTTP request.
 * <p>
 * You can visit the RoutingContext instance in the whole router chain.
 * It provides HTTP request/response API and allows you to maintain arbitrary data that lives for the lifetime of the context.
 * Contexts are discarded once they have been routed to the handler for the request.
 * <p>
 * The context also provides access to the Session, cookies and body for the request, given the correct handlers in the application.
 *
 * @author Pengtao Qiu
 */
public interface RoutingContext extends Closeable {

    Object getAttribute(String key);

    Object setAttribute(String key, Object value);

    Object removeAttribute(String key);

    ConcurrentHashMap<String, Object> getAttributes();

    SimpleResponse getResponse();

    SimpleResponse getAsyncResponse();

    SimpleRequest getRequest();

    String getRouterParameter(String name);

    default Optional<String> getRouterParamOpt(String name) {
        return Optional.ofNullable(getRouterParameter(name));
    }

    RoutingContext content(Action1<ByteBuffer> content);

    RoutingContext contentComplete(Action1<SimpleRequest> contentComplete);

    RoutingContext messageComplete(Action1<SimpleRequest> messageComplete);

    boolean isAsynchronousRead();

    boolean next();

    boolean hasNext();

    <T> RoutingContext complete(Promise<T> promise);

    <T> boolean next(Promise<T> promise);

    default <T> CompletableFuture<T> nextFuture() {
        Promise.Completable<T> completable = new Promise.Completable<>();
        next(completable);
        return completable;
    }

    default <T> CompletableFuture<T> complete() {
        Promise.Completable<T> completable = new Promise.Completable<>();
        complete(completable);
        return completable;
    }

    <T> void succeed(T t);

    void fail(Throwable x);


    // request wrap
    default String getMethod() {
        return getRequest().getMethod();
    }

    default HttpURI getURI() {
        return getRequest().getURI();
    }

    default HttpVersion getHttpVersion() {
        return getRequest().getHttpVersion();
    }

    default HttpFields getFields() {
        return getRequest().getFields();
    }

    default long getContentLength() {
        return getRequest().getContentLength();
    }

    default List<Cookie> getCookies() {
        return getRequest().getCookies();
    }


    // response wrap
    default RoutingContext setStatus(int status) {
        getResponse().setStatus(status);
        return this;
    }

    default RoutingContext setReason(String reason) {
        getResponse().setReason(reason);
        return this;
    }

    default RoutingContext setHttpVersion(HttpVersion httpVersion) {
        getResponse().setHttpVersion(httpVersion);
        return this;
    }

    default RoutingContext put(HttpHeader header, String value) {
        getResponse().put(header, value);
        return this;
    }

    default RoutingContext put(String header, String value) {
        getResponse().put(header, value);
        return this;
    }

    default RoutingContext add(HttpHeader header, String value) {
        getResponse().add(header, value);
        return this;
    }

    default RoutingContext add(String name, String value) {
        getResponse().add(name, value);
        return this;
    }

    default RoutingContext addCookie(Cookie cookie) {
        getResponse().addCookie(cookie);
        return this;
    }

    default RoutingContext write(String value) {
        getResponse().write(value);
        return this;
    }

    default RoutingContext writeJson(Object object) {
        put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON.asString()).write(Json.toJson(object));
        return this;
    }

    default RoutingContext end(String value) {
        return write(value).end();
    }

    default RoutingContext end() {
        getResponse().end();
        return this;
    }

    default RoutingContext write(byte[] b, int off, int len) {
        getResponse().write(b, off, len);
        return this;
    }

    default RoutingContext write(byte[] b) {
        return write(b, 0, b.length);
    }

    default RoutingContext end(byte[] b) {
        return write(b).end();
    }


    // HTTP body API
    String getParameter(String name);

    default Optional<String> getParamOpt(String name) {
        return Optional.ofNullable(getParameter(name));
    }

    List<String> getParameterValues(String name);

    Map<String, List<String>> getParameterMap();

    Collection<Part> getParts();

    Part getPart(String name);

    InputStream getInputStream();

    BufferedReader getBufferedReader();

    String getStringBody(String charset);

    String getStringBody();

    <T> T getJsonBody(Class<T> clazz);

    <T> T getJsonBody(GenericTypeReference<T> typeReference);

    JsonObject getJsonObjectBody();

    JsonArray getJsonArrayBody();


    // HTTP session API
    default HTTPSession getSessionNow() {
        return getSession().getNow(null);
    }

    default HTTPSession getSessionNow(boolean create) {
        return getSession(create).getNow(null);
    }

    default int getSessionSizeNow() {
        return getSessionSize().getNow(0);
    }

    default boolean removeSessionNow() {
        return removeSession().getNow(false);
    }

    default boolean updateSessionNow(HTTPSession httpSession) {
        return updateSession(httpSession).getNow(false);
    }

    CompletableFuture<HTTPSession> getSession();

    CompletableFuture<HTTPSession> getSession(boolean create);

    CompletableFuture<Integer> getSessionSize();

    CompletableFuture<Boolean> removeSession();

    CompletableFuture<Boolean> updateSession(HTTPSession httpSession);

    boolean isRequestedSessionIdFromURL();

    boolean isRequestedSessionIdFromCookie();

    String getRequestedSessionId();

    // Template API
    void renderTemplate(String resourceName, Object scope);

    void renderTemplate(String resourceName, Object[] scopes);

    void renderTemplate(String resourceName, List<Object> scopes);

}
