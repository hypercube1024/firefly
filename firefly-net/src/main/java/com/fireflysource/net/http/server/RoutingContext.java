package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.model.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A new routing context instance creates when the server receives an HTTP request.
 * <p>
 * You can visit the RoutingContext instance in the whole router chain.
 * It provides HTTP request/response API and allows you to maintain data that lives for the lifetime of the context.
 * Contexts discarded once they have been routed to the handler for the request.
 * <p>
 * The context also provides access to the Session, cookies and body for the request, given the correct handlers in the application.
 *
 * @author Pengtao Qiu
 */
public interface RoutingContext {

    /**
     * Get the attribute value.
     *
     * @param key The attribute key.
     * @return The value.
     */
    Object getAttribute(String key);

    /**
     * Set the attribute value.
     *
     * @param key   The attribute key.
     * @param value The value.
     * @return The old value if exists.
     */
    Object setAttribute(String key, Object value);

    /**
     * Remove the value.
     *
     * @param key The attribute key.
     * @return The old value if exists.
     */
    Object removeAttribute(String key);

    /**
     * Get all attributes.
     *
     * @return All attributes.
     */
    Map<String, Object> getAttributes();

    /**
     * Get HTTP request.
     *
     * @return The HTTP request.
     */
    HttpServerRequest getRequest();

    /**
     * Get HTTP response.
     *
     * @return The HTTP response.
     */
    HttpServerResponse getResponse();


    /**
     * Get the parameter value. If you bind the parameter name for the path.
     *
     * @param name The path parameter name.
     * @return The value.
     */
    String getPathParameter(String name);

    /**
     * Get the parameter value. If you bind the wildcard for the path.
     *
     * @param index The wildcard index.
     * @return The value.
     */
    String getPathParameter(int index);

    /**
     * Get the path parameter by the regex group index. If you register the path using regex.
     *
     * @param index The regex group index.
     * @return The value.
     */
    String getPathParameterByRegexGroup(int index);

    /**
     * Get the URL query string.
     *
     * @param name The URL query parameter name.
     * @return The URL query parameter value.
     */
    String getQueryString(String name);

    /**
     * Get the URL query strings.
     *
     * @param name The URL query parameter name.
     * @return The URL query parameter values.
     */
    List<String> getQueryStrings(String name);

    /**
     * Get all URL query strings.
     *
     * @return All URL query strings.
     */
    Map<String, List<String>> getQueryStrings();

    /**
     * Get the web form input value.
     *
     * @param name The form input name.
     * @return The value.
     */
    String getFormInput(String name);

    /**
     * Get the web form input values.
     *
     * @param name The web form input name.
     * @return The values.
     */
    List<String> getFormInputs(String name);

    /**
     * Get all web form inputs.
     *
     * @return All web form inputs.
     */
    Map<String, List<String>> getFormInputs();

    /**
     * Get the HTTP body and convert it to the UTF-8 string.
     *
     * @return The HTTP body string.
     */
    default String getStringBody() {
        return getRequest().getStringBody();
    }

    /**
     * Get the HTTP body and convert the specified charset string.
     *
     * @param charset The charset of the HTTP body string.
     * @return The HTTP body string.
     */
    default String getStringBody(Charset charset) {
        return getRequest().getStringBody(charset);
    }

    /**
     * Get the HTTP body raw binary data.
     *
     * @return The HTTP body raw binary data.
     */
    default List<ByteBuffer> getBody() {
        return getRequest().getBody();
    }

    /**
     * Get HTTP request multi-part content.
     *
     * @param name The part name.
     * @return The HTTP request multi-part content.
     */
    MultiPart getPart(String name);

    /**
     * Get all HTTP request multi-part content.
     *
     * @return All HTTP request multi-part content.
     */
    List<MultiPart> getParts();

    /**
     * Set HTTP request content handler.
     *
     * @param contentHandler HTTP request content handler.
     * @return The routing context.
     */
    RoutingContext contentHandler(HttpServerContentHandler contentHandler);


    /**
     * Get HTTP request method.
     *
     * @return The HTTP request method.
     */
    default String getMethod() {
        return getRequest().getMethod();
    }

    /**
     * Get HTTP request URI.
     *
     * @return The HTTP request URI.
     */
    default HttpURI getURI() {
        return getRequest().getURI();
    }

    /**
     * Get HTTP request version.
     *
     * @return The HTTP request version.
     */
    default HttpVersion getHttpVersion() {
        return getRequest().getHttpVersion();
    }

    /**
     * Get HTTP request headers.
     *
     * @return The HTTP request headers.
     */
    default HttpFields getHttpFields() {
        return getRequest().getHttpFields();
    }

    /**
     * Get HTTP request content length.
     *
     * @return The HTTP request content length.
     */
    default long getContentLength() {
        return getRequest().getContentLength();
    }

    /**
     * Get HTTP request cookies.
     *
     * @return The HTTP request cookies.
     */
    default List<Cookie> getCookies() {
        return getRequest().getCookies();
    }

    /**
     * Set HTTP response status.
     *
     * @param status The HTTP response status.
     * @return The routing context.
     */
    default RoutingContext setStatus(int status) {
        getResponse().setStatus(status);
        return this;
    }

    /**
     * Set HTTP response reason.
     *
     * @param reason The HTTP response reason.
     * @return The routing context.
     */
    default RoutingContext setReason(String reason) {
        getResponse().setReason(reason);
        return this;
    }

    /**
     * Set HTTP response version.
     *
     * @param httpVersion The HTTP response version.
     * @return The routing context.
     */
    default RoutingContext setHttpVersion(HttpVersion httpVersion) {
        getResponse().setHttpVersion(httpVersion);
        return this;
    }

    /**
     * Put HTTP response header.
     *
     * @param header The HTTP header.
     * @param value  The value.
     * @return The routing context.
     */
    default RoutingContext put(HttpHeader header, String value) {
        getResponse().getHttpFields().put(header, value);
        return this;
    }

    /**
     * Put HTTP response header.
     *
     * @param header The HTTP header.
     * @param value  The value.
     * @return The routing context.
     */
    default RoutingContext put(HttpHeader header, HttpHeaderValue value) {
        getResponse().getHttpFields().put(header, value);
        return this;
    }

    /**
     * Put HTTP response header.
     *
     * @param header The HTTP header.
     * @param value  The value.
     * @return The routing context.
     */
    default RoutingContext put(String header, String value) {
        getResponse().getHttpFields().put(header, value);
        return this;
    }

    /**
     * Add HTTP response header.
     *
     * @param header The HTTP header.
     * @param value  The value.
     * @return The routing context.
     */
    default RoutingContext add(HttpHeader header, String value) {
        getResponse().getHttpFields().add(header, value);
        return this;
    }

    /**
     * Add HTTP response header.
     *
     * @param header The HTTP header.
     * @param value  The value.
     * @return The routing context.
     */
    default RoutingContext add(HttpHeader header, HttpHeaderValue value) {
        getResponse().getHttpFields().add(header, value);
        return this;
    }

    /**
     * Add HTTP response header.
     *
     * @param header The HTTP header.
     * @param value  The value.
     * @return The routing context.
     */
    default RoutingContext add(String header, String value) {
        getResponse().getHttpFields().add(header, value);
        return this;
    }

    /**
     * Add HTTP response cookie.
     *
     * @param cookie The HTTP response cookie.
     * @return The routing context.
     */
    default RoutingContext addCookie(Cookie cookie) {
        getResponse().getCookies().add(cookie);
        return this;
    }

    /**
     * Set the HTTP response content provider.
     *
     * @param contentProvider HTTP response content provider.
     * @return The routing context.
     */
    RoutingContext contentProvider(HttpServerContentProvider contentProvider);

    /**
     * Write string to the client.
     *
     * @param value The response content.
     * @return The routing context.
     */
    RoutingContext write(String value);

    /**
     * Write the response content.
     *
     * @param byteBuffer The response content.
     * @return The routing context.
     */
    RoutingContext write(ByteBuffer byteBuffer);

    /**
     * Write the response content.
     *
     * @param byteBufferList The response content list.
     * @param offset         The offset within the buffer list of the first buffer into which
     *                       bytes are to be transferred; must be non-negative and no larger than
     *                       byteBuffers.length.
     * @param length         The maximum number of buffers to be accessed; must be non-negative
     *                       and no larger than byteBuffers.length - offset.
     * @return The routing context.
     */
    RoutingContext write(List<ByteBuffer> byteBufferList, int offset, int length);

    /**
     * Write the response content.
     *
     * @param byteBuffers The response content array.
     * @param offset      The offset within the buffer array of the first buffer into which
     *                    bytes are to be transferred; must be non-negative and no larger than
     *                    byteBuffers.length.
     * @param length      The maximum number of buffers to be accessed; must be non-negative
     *                    and no larger than byteBuffers.length - offset.
     * @return The routing context.
     */
    RoutingContext write(ByteBuffer[] byteBuffers, int offset, int length);

    /**
     * End the HTTP response.
     *
     * @return The response future result.
     */
    CompletableFuture<Void> end();

    /**
     * Write the value and end the HTTP response.
     *
     * @param value The HTTP response content.
     * @return The response future result.
     */
    CompletableFuture<Void> end(String value);

    /**
     * Write the redirect response to the client.
     *
     * @param url The redirect URL.
     * @return The response future result.
     */
    CompletableFuture<Void> redirect(String url);


    /**
     * If true, the router chain has next handler.
     *
     * @return If true, the router chain has next handler.
     */
    boolean hasNext();

    /**
     * Execute the next handler of the router chain.
     *
     * @param <T> The handler result type.
     * @return The handler future result.
     */
    <T> CompletableFuture<T> next();

    /**
     * Get the HTTP server connection.
     *
     * @return The HTTP server connection.
     */
    HttpServerConnection getConnection();

}
