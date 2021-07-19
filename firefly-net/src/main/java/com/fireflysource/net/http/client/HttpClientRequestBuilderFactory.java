package com.fireflysource.net.http.client;

import com.fireflysource.net.http.common.exception.URISyntaxRuntimeException;
import com.fireflysource.net.http.common.model.HttpMethod;
import com.fireflysource.net.http.common.model.HttpURI;

import java.net.URISyntaxException;
import java.net.URL;

/**
 * Create a new HTTP client request builder.
 */
public interface HttpClientRequestBuilderFactory {

    /**
     * Create a RequestBuilder with GET method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    default HttpClientRequestBuilder get(String url) {
        return request(HttpMethod.GET, url);
    }

    /**
     * Create a RequestBuilder with POST method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    default HttpClientRequestBuilder post(String url) {
        return request(HttpMethod.POST, url);
    }

    /**
     * Create a RequestBuilder with HEAD method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    default HttpClientRequestBuilder head(String url) {
        return request(HttpMethod.HEAD, url);
    }

    /**
     * Create a RequestBuilder with PUT method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    default HttpClientRequestBuilder put(String url) {
        return request(HttpMethod.PUT, url);
    }

    /**
     * Create a RequestBuilder with DELETE method and URL.
     *
     * @param url The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    default HttpClientRequestBuilder delete(String url) {
        return request(HttpMethod.DELETE, url);
    }

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method The HTTP method.
     * @param url    The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    default HttpClientRequestBuilder request(HttpMethod method, String url) {
        return request(method.getValue(), url);
    }

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method The HTTP method.
     * @param url    The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    default HttpClientRequestBuilder request(String method, String url) {
        return request(method, new HttpURI(url));
    }

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method The HTTP method.
     * @param url    The request URL.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    default HttpClientRequestBuilder request(String method, URL url) {
        try {
            return request(method, new HttpURI(url.toURI()));
        } catch (URISyntaxException e) {
            throw new URISyntaxRuntimeException("URI syntax error", e);
        }
    }

    /**
     * Create a RequestBuilder with HTTP method and URL.
     *
     * @param method  The HTTP method.
     * @param httpURI The HTTP URI.
     * @return A new RequestBuilder that helps you to build an HTTP request.
     */
    HttpClientRequestBuilder request(String method, HttpURI httpURI);
}
