package com.fireflysource.net.http.client;

import com.fireflysource.net.http.common.model.Cookie;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpVersion;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Pengtao Qiu
 */
public interface HttpClientResponse {

    /**
     * Get the HTTP response status code.
     *
     * @return The HTTP response status code.
     */
    int getStatus();

    /**
     * Get the textual description associated with the numeric status code.
     *
     * @return The textual description associated with the numeric status code.
     */
    String getReason();

    /**
     * Get the HTTP version of the current HTTP connection.
     *
     * @return the HTTP version of the current HTTP connection.
     */
    HttpVersion getHttpVersion();

    /**
     * Get the HTTP header fields.
     *
     * @return The HTTP header fields.
     */
    HttpFields getHttpFields();

    /**
     * Get the cookies.
     *
     * @return The cookies.
     */
    List<Cookie> getCookies();

    /**
     * Get the content length.
     *
     * @return The content length.
     */
    long getContentLength();

    /**
     * Get the HTTP trailer fields.
     *
     * @return The HTTP trailer fields.
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
    String getStringBody(String charset);

    /**
     * Get the HTTP body raw binary data.
     *
     * @return The HTTP body raw binary data.
     */
    List<ByteBuffer> getBody();
}
