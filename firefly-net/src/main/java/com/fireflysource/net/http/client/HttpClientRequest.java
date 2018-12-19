package com.fireflysource.net.http.client;

import com.fireflysource.common.func.Callback;
import com.fireflysource.net.http.common.exception.BadMessageException;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpURI;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The HTTP client request.
 *
 * @author Pengtao Qiu
 */
public interface HttpClientRequest {

    /**
     * Get the HTTP method.
     *
     * @return the HTTP method
     */
    String getMethod();

    /**
     * Set the HTTP method.
     *
     * @param method the HTTP method to set
     */
    void setMethod(String method);

    /**
     * Get the HTTP URI.
     *
     * @return the HTTP URI
     */
    HttpURI getURI();

    /**
     * Set the HTTP URI.
     *
     * @param uri the HTTP URI
     */
    void setURI(HttpURI uri);

    /**
     * Set the query parameters.
     *
     * @param queryParameters The query parameters.
     */
    void setQueryParameters(Map<String, List<String>> queryParameters);

    /**
     * Get the query parameters.
     *
     * @return The query parameters.ø
     */
    Map<String, List<String>> getQueryParameters();

    /**
     * Get the HTTP header fields.
     *
     * @return the HTTP header fields.
     */
    HttpFields getHttpFields();

    /**
     * Set the HTTP header fields.
     *
     * @param httpFields the HTTP header fields.
     */
    void setHttpFields(HttpFields httpFields);

    /**
     * Get the HTTP trailers.
     *
     * @return The HTTP trailers.
     */
    Supplier<HttpFields> getTrailerSupplier();

    /**
     * Set the HTTP trailers.
     *
     * @param trailers The HTTP trailers.
     */
    void setTrailerSupplier(Supplier<HttpFields> trailers);

    /**
     * Set the content provider. When you submit the request, the HTTP client will send the data that read from the content provider.
     *
     * @param contentProvider When you submit the request, the HTTP client will send the data that read from the content provider.
     */
    void setContentProvider(HttpClientContentProvider contentProvider);

    /**
     * Get the content provider.
     *
     * @return the content provider.
     */
    HttpClientContentProvider getHttpClientContentProvider();

    /**
     * Set the HTTP header complete callback.
     *
     * @param headerComplete The HTTP header complete callback. When the HTTP client receives all HTTP headers,
     *                       it will execute this action.
     */
    void setHeaderComplete(Consumer<HttpClientResponse> headerComplete);

    /**
     * Set the HTTP content receiving callback.
     *
     * @param contentHandler The HTTP content receiving callback. When the HTTP client receives the HTTP body data,
     *                       it will execute this action. This action will be executed many times.
     */
    void setContentHandler(HttpClientContentHandler contentHandler);

    /**
     * Set the HTTP content complete callback.
     *
     * @param contentComplete The HTTP content complete callback. When the HTTP client receives the HTTP body finish,
     *                        it will execute this action.
     */
    void setContentComplete(Consumer<HttpClientResponse> contentComplete);

    /**
     * Set the HTTP message complete callback.
     *
     * @param messageComplete The HTTP message complete callback. When the HTTP client receives the complete HTTP message
     *                        that contains HTTP headers and body, it will execute this action.
     */
    void setMessageComplete(Consumer<HttpClientResponse> messageComplete);

    /**
     * Set the bad message callback.
     *
     * @param badMessage The bad message callback. When the HTTP client parses an incorrect message format,
     *                   it will execute this action.
     */
    void setBadMessage(Consumer<BadMessageException> badMessage);

    /**
     * Set the early EOF callback.
     *
     * @param earlyEof The early EOF callback. When the HTTP client encounters an error, it will execute this action.
     */
    void setEarlyEof(Callback earlyEof);
}
