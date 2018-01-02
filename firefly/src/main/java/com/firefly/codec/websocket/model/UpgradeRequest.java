package com.firefly.codec.websocket.model;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.codec.http2.model.HttpURI;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * The HTTP Upgrade to WebSocket Request
 */
public interface UpgradeRequest {
    /**
     * Add WebSocket Extension Configuration(s) to Upgrade Request.
     * <p>
     * This is merely the list of requested Extensions to use, see {@link UpgradeResponse#getExtensions()} for what was
     * negotiated
     *
     * @param configs the configuration(s) to add
     */
    void addExtensions(ExtensionConfig... configs);

    /**
     * Add WebSocket Extension Configuration(s) to request
     * <p>
     * This is merely the list of requested Extensions to use, see {@link UpgradeResponse#getExtensions()} for what was
     * negotiated
     *
     * @param configs the configuration(s) to add
     */
    void addExtensions(String... configs);

    /**
     * Get the list of Cookies on the Upgrade request
     *
     * @return the list of Cookies
     */
    List<Cookie> getCookies();

    /**
     * Get the list of WebSocket Extension Configurations for this Upgrade Request.
     * <p>
     * This is merely the list of requested Extensions to use, see {@link UpgradeResponse#getExtensions()} for what was
     * negotiated
     *
     * @return the list of Extension configurations (in the order they were specified)
     */
    List<ExtensionConfig> getExtensions();

    /**
     * Get a specific Header value from Upgrade Request
     *
     * @param name the name of the header
     * @return the value of the header (null if header does not exist)
     */
    String getHeader(String name);

    /**
     * Get the specific Header value, as an <code>int</code>, from the Upgrade Request.
     *
     * @param name the name of the header
     * @return the value of the header as an <code>int</code> (-1 if header does not exist)
     * @throws NumberFormatException if unable to parse value as an int.
     */
    int getHeaderInt(String name);

    /**
     * Get the headers as a Map of keys to value lists.
     *
     * @return the headers
     */
    Map<String, List<String>> getHeaders();

    /**
     * Get the specific header values (for multi-value headers)
     *
     * @param name the header name
     * @return the value list (null if no header exists)
     */
    List<String> getHeaders(String name);

    /**
     * The host of the Upgrade Request URI
     *
     * @return host of the request URI
     */
    String getHost();

    /**
     * The HTTP version used for this Upgrade Request
     * <p>
     * As of <a href="http://tools.ietf.org/html/rfc6455">RFC6455 (December 2011)</a> this is always
     * <code>HTTP/1.1</code>
     *
     * @return the HTTP Version used
     */
    String getHttpVersion();

    /**
     * The HTTP method for this Upgrade Request.
     * <p>
     * As of <a href="http://tools.ietf.org/html/rfc6455">RFC6455 (December 2011)</a> this is always <code>GET</code>
     *
     * @return the HTTP method used
     */
    String getMethod();

    /**
     * The WebSocket Origin of this Upgrade Request
     * <p>
     * See <a href="http://tools.ietf.org/html/rfc6455#section-10.2">RFC6455: Section 10.2</a> for details.
     * <p>
     * Equivalent to {@link #getHeader(String)} passed the "Origin" header.
     *
     * @return the Origin header
     */
    String getOrigin();

    /**
     * Returns a map of the query parameters of the request.
     *
     * @return a unmodifiable map of query parameters of the request.
     */
    Map<String, List<String>> getParameterMap();

    /**
     * Get the WebSocket Protocol Version
     * <p>
     * As of <a href="http://tools.ietf.org/html/rfc6455#section-11.6">RFC6455</a>, Firefly only supports version
     * <code>13</code>
     *
     * @return the WebSocket protocol version
     */
    String getProtocolVersion();

    /**
     * Get the Query String of the request URI.
     *
     * @return the request uri query string
     */
    String getQueryString();

    /**
     * Get the Request URI
     *
     * @return the request URI
     */
    HttpURI getRequestURI();

    /**
     * Access the Servlet HTTP Session (if present)
     * <p>
     * Note: Never present on a Client UpgradeRequest.
     *
     * @return the Servlet HTTPSession on server side UpgradeRequests
     */
    Object getSession();

    /**
     * Get the list of offered WebSocket sub-protocols.
     *
     * @return the list of offered sub-protocols
     */
    List<String> getSubProtocols();

    /**
     * Get the User Principal for this request.
     * <p>
     * Only applicable when using UpgradeRequest from server side.
     *
     * @return the user principal
     */
    Principal getUserPrincipal();

    /**
     * Test if a specific sub-protocol is offered
     *
     * @param test the sub-protocol to test for
     * @return true if sub-protocol exists on request
     */
    boolean hasSubProtocol(String test);

    /**
     * Test if supplied Origin is the same as the Request
     *
     * @param test the supplied origin
     * @return true if the supplied origin matches the request origin
     */
    boolean isOrigin(String test);

    /**
     * Test if connection is secure.
     *
     * @return true if connection is secure.
     */
    boolean isSecure();

    /**
     * Set the list of Cookies on the request
     *
     * @param cookies the cookies to use
     */
    void setCookies(List<Cookie> cookies);

    /**
     * Set the list of WebSocket Extension configurations on the request.
     *
     * @param configs the list of extension configurations
     */
    void setExtensions(List<ExtensionConfig> configs);

    /**
     * Set a specific header with multi-value field
     * <p>
     * Overrides any previous value for this named header
     *
     * @param name   the name of the header
     * @param values the multi-value field
     */
    void setHeader(String name, List<String> values);

    /**
     * Set a specific header value
     * <p>
     * Overrides any previous value for this named header
     *
     * @param name  the header to set
     * @param value the value to set it to
     */
    void setHeader(String name, String value);

    /**
     * Sets multiple headers on the request.
     * <p>
     * Only sets those headers provided, does not remove
     * headers that exist on request and are not provided in the
     * parameter for this method.
     * <p>
     * Convenience method vs calling {@link #setHeader(String, List)} multiple times.
     *
     * @param headers the headers to set
     */
    void setHeaders(Map<String, List<String>> headers);

    /**
     * Set the HTTP Version to use.
     * <p>
     * As of <a href="http://tools.ietf.org/html/rfc6455">RFC6455 (December 2011)</a> this should always be
     * <code>HTTP/1.1</code>
     *
     * @param httpVersion the HTTP version to use.
     */
    void setHttpVersion(String httpVersion);

    /**
     * Set the HTTP method to use.
     * <p>
     * As of <a href="http://tools.ietf.org/html/rfc6455">RFC6455 (December 2011)</a> this is always <code>GET</code>
     *
     * @param method the HTTP method to use.
     */
    void setMethod(String method);

    /**
     * Set the Request URI to use for this request.
     * <p>
     * Must be an absolute URI with scheme <code>'ws'</code> or <code>'wss'</code>
     *
     * @param uri the Request URI
     */
    void setRequestURI(HttpURI uri);

    /**
     * Set the Session associated with this request.
     * <p>
     * Typically used to associate the Servlet HttpSession object.
     *
     * @param session the session object to associate with this request
     */
    void setSession(Object session);

    /**
     * Set the offered WebSocket Sub-Protocol list.
     *
     * @param protocols the offered sub-protocol list
     */
    void setSubProtocols(List<String> protocols);

    /**
     * Set the offered WebSocket Sub-Protocol list.
     *
     * @param protocols the offered sub-protocol list
     */
    void setSubProtocols(String... protocols);

}
