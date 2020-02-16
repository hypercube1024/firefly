package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.model.Cookie;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpURI;
import com.fireflysource.net.http.common.model.HttpVersion;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The HTTP request.
 *
 * @author Pengtao Qiu
 */
public interface HttpServerRequest {

    /**
     * Get the HTTP method.
     *
     * @return The HTTP method.
     */
    String getMethod();

    /**
     * Get the HTTP URI.
     *
     * @return The HTTP URI.
     */
    HttpURI getURI();

    /**
     * Get the HTTP version.
     *
     * @return The HTTP version.
     */
    HttpVersion getHttpVersion();

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
     * Get the HTTP header fields.
     *
     * @return The HTTP header fields.
     */
    HttpFields getHttpFields();

    /**
     * Get the HTTP cookies.
     *
     * @return The HTTP cookies.
     */
    List<Cookie> getCookies();

    /**
     * Get the content length.
     *
     * @return The content length.
     */
    long getContentLength();

    /**
     * Get the HTTP trailers.
     *
     * @return The HTTP trailers.
     */
    Supplier<HttpFields> getTrailerSupplier();

    /**
     * Get the HTTP body and convert it to the UTF-8 string.
     *
     * @return The HTTP body string.
     */
    String getStringBody();

    /**
     * Get the HTTP body and convert the specified charset string.
     *
     * @param charset The charset of the HTTP body string.
     * @return The HTTP body string.
     */
    String getStringBody(Charset charset);

    /**
     * Get the HTTP body raw binary data.
     *
     * @return The HTTP body raw binary data.
     */
    List<ByteBuffer> getBody();

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
     * Get HTTP request content handler.
     *
     * @return HTTP content handler.
     */
    HttpServerContentHandler getContentHandler();

    /**
     * Set HTTP request content handler.
     *
     * @param contentHandler HTTP request content handler.
     */
    void setContentHandler(HttpServerContentHandler contentHandler);
}
