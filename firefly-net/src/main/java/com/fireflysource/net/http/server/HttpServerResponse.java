package com.fireflysource.net.http.server;

import com.fireflysource.common.io.AsyncCloseable;
import com.fireflysource.net.http.common.model.Cookie;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpVersion;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The HTTP response.
 *
 * @author Pengtao Qiu
 */
public interface HttpServerResponse extends AsyncCloseable {

    /**
     * Get the HTTP response status code.
     *
     * @return The HTTP response status code.
     */
    int getStatus();

    /**
     * Set the HTTP response status code.
     *
     * @param status The HTTP response status code.
     */
    void setStatus(int status);

    /**
     * Get the textual description associated with the numeric status code.
     *
     * @return The textual description associated with the numeric status code.
     */
    String getReason();

    /**
     * Set the textual description associated with the numeric status code.
     *
     * @param reason The textual description associated with the numeric status code.
     */
    void setReason(String reason);

    /**
     * Get the HTTP version of the current HTTP connection.
     *
     * @return The HTTP version of the current HTTP connection.
     */
    HttpVersion getHttpVersion();

    /**
     * Set the HTTP version of the current HTTP connection.
     *
     * @param httpVersion The HTTP version of the current HTTP connection.
     */
    void setHttpVersion(HttpVersion httpVersion);

    /**
     * Get the HTTP header fields.
     *
     * @return The HTTP header fields.
     */
    HttpFields getHttpFields();

    /**
     * Set the HTTP header fields.
     *
     * @param httpFields The HTTP header fields.
     */
    void setHttpFields(HttpFields httpFields);

    /**
     * Get the cookies.
     *
     * @return The cookies.
     */
    List<Cookie> getCookies();

    /**
     * Set the cookies.
     *
     * @param cookies The cookies.
     */
    void setCookies(List<Cookie> cookies);

    /**
     * Get the HTTP trailer fields.
     *
     * @return The HTTP trailer fields.
     */
    Supplier<HttpFields> getTrailerSupplier();

    /**
     * Set the HTTP trailer fields.
     *
     * @param supplier The HTTP trailer fields supplier.
     */
    void setTrailerSupplier(Supplier<HttpFields> supplier);

    /**
     * Get the content provider.
     *
     * @return the content provider.
     */
    HttpServerContentProvider getContentProvider();

    /**
     * Set the content provider. When you commit the response, the HTTP server will send the data that read from the content provider.
     * If you set content provider after commit response, this method will throw IllegalStateException.
     *
     * @param contentProvider When you commit the response, the HTTP server will send the data that read from the content provider.
     */
    void setContentProvider(HttpServerContentProvider contentProvider);

    /**
     * Get the output channel. It can write data to the client.
     * If you get output channel before commit response, this method will throw IllegalStateException.
     *
     * @return The output channel.
     */
    HttpServerOutputChannel getOutputChannel();

    /**
     * Commit the response. If you set the content provider, the server will output the data from the content provider,
     * or else you can write data using the output channel.
     *
     * @return The future result.
     */
    CompletableFuture<Void> commit();

    /**
     * If true, the http response has committed.
     *
     * @return If true, the http response has committed.
     */
    boolean isCommitted();

    /**
     * Response 100 continue.
     *
     * @return The future result.
     */
    CompletableFuture<Void> response100Continue();

    /**
     * Response 200 connection established.
     *
     * @return The future result.
     */
    CompletableFuture<Void> response200ConnectionEstablished();
}
