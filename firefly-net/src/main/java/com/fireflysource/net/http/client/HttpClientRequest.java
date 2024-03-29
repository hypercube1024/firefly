package com.fireflysource.net.http.client;

import com.fireflysource.net.http.common.codec.UrlEncoded;
import com.fireflysource.net.http.common.model.Cookie;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpURI;
import com.fireflysource.net.http.common.model.HttpVersion;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
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
     * @return The HTTP method.
     */
    String getMethod();

    /**
     * Set the HTTP method.
     *
     * @param method The HTTP method.
     */
    void setMethod(String method);

    /**
     * Get the HTTP URI.
     *
     * @return The HTTP URI.
     */
    HttpURI getURI();

    /**
     * Set the HTTP URI.
     *
     * @param uri The HTTP URI.
     */
    void setURI(HttpURI uri);

    /**
     * Get the HTTP version.
     *
     * @return The HTTP version.
     */
    HttpVersion getHttpVersion();

    /**
     * Set the HTTP version.
     *
     * @param httpVersion The HTTP version.
     */
    void setHttpVersion(HttpVersion httpVersion);

    /**
     * Get URL query strings.
     *
     * @return The URL query strings.
     */
    UrlEncoded getQueryStrings();

    /**
     * Set URL query strings.
     *
     * @param queryStrings The URL query strings.
     */
    void setQueryStrings(UrlEncoded queryStrings);

    /**
     * Get the web form inputs.
     *
     * @return The web form inputs.
     */
    UrlEncoded getFormInputs();

    /**
     * Set the web form inputs
     *
     * @param formInputs The web form inputs.
     */
    void setFormInputs(UrlEncoded formInputs);

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
     * Get the HTTP cookies.
     *
     * @return The HTTP cookies.
     */
    List<Cookie> getCookies();

    /**
     * Set the HTTP cookies.
     *
     * @param cookies The HTTP cookies.
     */
    void setCookies(List<Cookie> cookies);

    /**
     * Get the HTTP trailers.
     *
     * @return The HTTP trailers.
     */
    Supplier<HttpFields> getTrailerSupplier();

    /**
     * Set the HTTP trailers.
     *
     * @param trailerSupplier The HTTP trailers.
     */
    void setTrailerSupplier(Supplier<HttpFields> trailerSupplier);

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
    HttpClientContentProvider getContentProvider();

    /**
     * Set the HTTP content receiving handler.
     *
     * @param contentHandler The HTTP content receiving handler. When the HTTP client receives the HTTP body data,
     *                       it will execute this action. It be executed many times.
     */
    void setContentHandler(HttpClientContentHandler contentHandler);

    /**
     * Get the HTTP content receiving callback.
     *
     * @return The HTTP content receiving callback. When the HTTP client receives the HTTP body data,
     * it will execute this action. It will be executed many times.
     */
    HttpClientContentHandler getContentHandler();

    /**
     * Set the HTTP2 settings.
     *
     * @param settings The HTTP2 settings.
     */
    void setHttp2Settings(Map<Integer, Integer> settings);

    /**
     * Get the HTTP2 settings.
     *
     * @return The HTTP2 settings.
     */
    Map<Integer, Integer> getHttp2Settings();

    /**
     * Set the HTTP header response complete callback.
     *
     * @param headerComplete The HTTP header response complete callback.
     */
    void setHeaderComplete(BiConsumer<HttpClientRequest, HttpClientResponse> headerComplete);

    /**
     * Get the HTTP header response complete callback.
     *
     * @return The HTTP header response complete callback.
     */
    BiConsumer<HttpClientRequest, HttpClientResponse> getHeaderComplete();
}
