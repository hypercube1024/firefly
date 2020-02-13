package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.model.Cookie;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpURI;
import com.fireflysource.net.http.common.model.HttpVersion;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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
     * Get the HTTP content receiving handler.
     *
     * @return The HTTP content receiving callback. When the HTTP server receives the HTTP body data,
     * it will execute this action. It will be executed many times.
     */
    HttpServerContentHandler getContentHandler();

    /**
     * Set the HTTP content receiving handler.
     *
     * @param contentHandler The HTTP content receiving handler. When the HTTP sever receives the HTTP body data,
     *                       it will execute this action. It will be executed many times.
     */
    void setContentHandler(HttpServerContentHandler contentHandler);

}
