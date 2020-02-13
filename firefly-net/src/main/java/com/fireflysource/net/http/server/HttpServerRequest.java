package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.model.Cookie;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpURI;
import com.fireflysource.net.http.common.model.HttpVersion;

import java.nio.ByteBuffer;
import java.util.List;
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
     * Get the HTTP body raw binary data.
     *
     * @return The HTTP body raw binary data.
     */
    List<ByteBuffer> getBody();

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
