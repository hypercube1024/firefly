package com.fireflysource.net.http.server;

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
}
