package com.firefly.server.http2.router;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.oauth2.exception.OAuthProblemException;
import com.firefly.codec.oauth2.model.*;
import com.firefly.codec.oauth2.model.message.types.ResponseType;
import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandlerLoader;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.firefly.codec.oauth2.model.OAuth.*;

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

    default int getConnectionId() {
        return getRequest().getConnection().getSessionId();
    }

    String getRouterParameter(String name);

    default String getWildcardMatchedResult(int index) {
        return getRouterParameter("param" + index);
    }

    default String getRegexGroup(int index) {
        return getRouterParameter("group" + index);
    }

    default String getPathParameter(String name) {
        return getRouterParameter(name);
    }

    default Optional<String> getRouterParamOpt(String name) {
        return Optional.ofNullable(getRouterParameter(name));
    }

    /**
     * Set the HTTP body packet receiving callback.
     *
     * @param content The HTTP body data receiving callback. When the server receives the HTTP body packet, it will be called.
     * @return RoutingContext
     */
    RoutingContext content(Action1<ByteBuffer> content);

    /**
     * Set the HTTP body packet complete callback.
     *
     * @param contentComplete The HTTP body packet complete callback.
     * @return RoutingContext
     */
    RoutingContext contentComplete(Action1<SimpleRequest> contentComplete);

    /**
     * Set the HTTP message complete callback.
     *
     * @param messageComplete the HTTP message complete callback.
     * @return RoutingContext
     */
    RoutingContext messageComplete(Action1<SimpleRequest> messageComplete);

    /**
     * If return true, it represents you has set a HTTP body data receiving callback.
     *
     * @return If return true, it represents you has set a HTTP body data receiving callback
     */
    boolean isAsynchronousRead();

    /**
     * Execute the next handler.
     *
     * @return If return false, it represents current handler is the last.
     */
    boolean next();

    /**
     * If return false, it represents current handler is the last.
     *
     * @return If return false, it represents current handler is the last.
     */
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
        put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.asString()).write(Json.toJson(object));
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

    default void redirect(String url) {
        setStatus(HttpStatus.FOUND_302).put(HttpHeader.LOCATION, url);
        DefaultErrorResponseHandlerLoader.getInstance().getHandler().render(this, HttpStatus.FOUND_302, null);
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

    CompletableFuture<HTTPSession> getSessionById(String id);

    CompletableFuture<HTTPSession> getSession();

    CompletableFuture<HTTPSession> getSession(boolean create);

    CompletableFuture<HTTPSession> getAndCreateSession(int maxAge);

    CompletableFuture<Integer> getSessionSize();

    CompletableFuture<Boolean> removeSessionById(String id);

    CompletableFuture<Boolean> removeSession();

    CompletableFuture<Boolean> updateSession(HTTPSession httpSession);

    boolean isRequestedSessionIdFromURL();

    boolean isRequestedSessionIdFromCookie();

    String getRequestedSessionId();

    String getSessionIdParameterName();

    // Template API
    void renderTemplate(String resourceName, Object scope);

    void renderTemplate(String resourceName, Object[] scopes);

    void renderTemplate(String resourceName, List<Object> scopes);

    default void renderTemplate(String resourceName) {
        renderTemplate(resourceName, Collections.emptyList());
    }


    // OAuth2 API

    /**
     * Get the OAuth2 authorization request.
     *
     * @return The authorization request.
     */
    AuthorizationRequest getAuthorizationRequest();

    /**
     * If the the response type is code in the authorization request, response the authorization code. The code is used to exchange the access token.
     *
     * @param code The authorization code.
     */
    default void redirectWithCode(String code) {
        AuthorizationRequest req = getAuthorizationRequest();
        if (!req.getResponseType().equals(ResponseType.CODE.toString())) {
            throw OAuth.oauthProblem(OAuthError.CodeResponse.UNSUPPORTED_RESPONSE_TYPE)
                       .description("The response type must be code.")
                       .state(req.getState());
        }

        String redirectUrl = req.getRedirectUri();
        UrlEncoded urlEncoded = new UrlEncoded();
        urlEncoded.add(OAUTH_STATE, req.getState());
        urlEncoded.add(OAUTH_CODE, code);
        redirect(redirectUrl + "?" + urlEncoded.encode(StandardCharsets.UTF_8, true));
    }

    /**
     * Response the authorization error.
     *
     * @param exception The authorization exception.
     */
    default void redirectAuthorizationError(OAuthProblemException exception) {
        AuthorizationRequest req = getAuthorizationRequest();
        String redirectUrl = req.getRedirectUri();
        UrlEncoded urlEncoded = new UrlEncoded();
        urlEncoded.add(OAuthError.OAUTH_ERROR, exception.getError());
        urlEncoded.add(OAUTH_STATE, exception.getState());
        urlEncoded.add(OAuthError.OAUTH_ERROR_DESCRIPTION, exception.getDescription());
        urlEncoded.add(OAuthError.OAUTH_ERROR_URI, exception.getUri());
        redirect(redirectUrl + "?" + urlEncoded.encode(StandardCharsets.UTF_8, true));
    }

    /**
     * Get the authorization code, the client will use the code to exchange the access token.
     *
     * @return The authorization code access token request.
     */
    AuthorizationCodeAccessTokenRequest getAuthorizationCodeAccessTokenRequest();

    /**
     * Get the username and password that are used to exchange the access token.
     *
     * @return The password access token request.
     */
    PasswordAccessTokenRequest getPasswordAccessTokenRequest();

    /**
     * Get the client credential that is used to exchange the access token.
     *
     * @return The client credential access token request.
     */
    ClientCredentialAccessTokenRequest getClientCredentialAccessTokenRequest();

    /**
     * Write the access token to the client.
     *
     * @param accessTokenResponse The access token that is used to get the resources.
     * @return The access token.
     */
    default RoutingContext writeAccessToken(AccessTokenResponse accessTokenResponse) {
        return writeJson(accessTokenResponse);
    }

    /**
     * If the the response type is token in the authorization request, response the access token.
     *
     * @param accessTokenResponse The access token response
     */
    default void redirectAccessToken(AccessTokenResponse accessTokenResponse) {
        AuthorizationRequest req = getAuthorizationRequest();
        if (!req.getResponseType().equals(ResponseType.TOKEN.toString())) {
            throw OAuth.oauthProblem(OAuthError.CodeResponse.UNSUPPORTED_RESPONSE_TYPE)
                       .description("The response type must be token.")
                       .state(req.getState());
        }

        String redirectUrl = req.getRedirectUri();
        UrlEncoded urlEncoded = new UrlEncoded();
        urlEncoded.add(OAUTH_ACCESS_TOKEN, accessTokenResponse.getAccessToken());
        urlEncoded.add(OAUTH_TOKEN_TYPE, accessTokenResponse.getTokenType());
        urlEncoded.add(OAUTH_EXPIRES_IN, accessTokenResponse.getExpiresIn().toString());
        urlEncoded.add(OAUTH_REFRESH_TOKEN, accessTokenResponse.getRefreshToken());
        urlEncoded.add(OAUTH_SCOPE, accessTokenResponse.getScope());
        urlEncoded.add(OAUTH_STATE, accessTokenResponse.getState());
        redirect(redirectUrl + "#" + urlEncoded.encode(StandardCharsets.UTF_8, true));
    }


}
