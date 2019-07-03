package com.fireflysource.net.http.client;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.http.common.model.HttpMethod;
import com.fireflysource.net.http.common.model.HttpURI;

import java.net.URL;

/**
 * @author Pengtao Qiu
 */
public interface HttpClient extends LifeCycle {

    /**
     * Create a RequestBuilder with GET method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder get(String url);

    /**
     * Create a RequestBuilder with POST method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder post(String url);

    /**
     * Create a RequestBuilder with HEAD method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder head(String url);

    /**
     * Create a RequestBuilder with PUT method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder put(String url);

    /**
     * Create a RequestBuilder with DELETE method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder delete(String url);

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method The HTTP method.
     * @param url    The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder request(HttpMethod method, String url);

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method The HTTP method.
     * @param url    The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder request(String method, String url);

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method The HTTP method.
     * @param url    The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder request(String method, URL url);

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method  The HTTP method.
     * @param httpURI The HTTP URI.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder request(String method, HttpURI httpURI);
}
