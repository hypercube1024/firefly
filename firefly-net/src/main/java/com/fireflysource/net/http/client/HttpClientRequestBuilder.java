package com.fireflysource.net.http.client;

import com.fireflysource.net.http.common.model.Cookie;
import com.fireflysource.net.http.common.model.HttpField;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpHeader;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The HTTP client request builder.
 *
 * @author Pengtao Qiu
 */
public interface HttpClientRequestBuilder {
    /**
     * Set the cookies.
     *
     * @param cookies The cookies.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder cookies(List<Cookie> cookies);

    /**
     * Put an HTTP field. It will replace the existed field.
     *
     * @param name The field name.
     * @param list The field values.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder put(String name, List<String> list);

    /**
     * Put an HTTP field. It will replace the existed field.
     *
     * @param header The field name.
     * @param value  The value.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder put(HttpHeader header, String value);

    /**
     * Put an HTTP field. It will replace the existed field.
     *
     * @param name  The field name.
     * @param value The value.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder put(String name, String value);

    /**
     * Put an HTTP field. It will replace the existed field.
     *
     * @param field The HTTP field.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder put(HttpField field);

    /**
     * Add some HTTP fields.
     *
     * @param fields The HTTP fields.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder addAll(HttpFields fields);

    /**
     * Add an HTTP field.
     *
     * @param field The HTTP field.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder add(HttpField field);

    /**
     * Set the HTTP trailers.
     *
     * @param trailerSupplier The HTTP trailers.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder trailerSupplier(Supplier<HttpFields> trailerSupplier);

    /**
     * Set the text HTTP body data.
     *
     * @param content The text HTTP body data.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder body(String content);

    /**
     * Set the text HTTP body data.
     *
     * @param content The text HTTP body data.
     * @param charset THe charset of the text.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder body(String content, Charset charset);

    /**
     * Set the HTTP body data. When you submit the request, the data will be sent.
     *
     * @param buffer The HTTP body data.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder body(ByteBuffer buffer);

    /**
     * Set the content provider. When you submit the request, the HTTP client will send the data that read from the content provider.
     *
     * @param contentProvider When you submit the request, the HTTP client will send the data that read from the content provider.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder contentProvider(HttpClientContentProvider contentProvider);

    /**
     * Add a multi-part mime content. Such as a file.
     *
     * @param name    The content name.
     * @param content The ContentProvider that helps you read the content.
     * @param fields  The header fields of the content.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder addFieldPart(String name, HttpClientContentProvider content, HttpFields fields);

    /**
     * Add a multi-part mime content. Such as a file.
     *
     * @param name     The content name.
     * @param fileName The content file name.
     * @param content  The ContentProvider that helps you read the content.
     * @param fields   The header fields of the content.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder addFilePart(String name, String fileName, HttpClientContentProvider content, HttpFields fields);

    /**
     * Add a value in an existed form parameter. The form content type is "application/x-www-form-urlencoded".
     *
     * @param name  The parameter name.
     * @param value The parameter value.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder addFormParam(String name, String value);

    /**
     * Add some values in an existed form parameter. The form content type is "application/x-www-form-urlencoded".
     *
     * @param name   The parameter name.
     * @param values The parameter values.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder addFormParam(String name, List<String> values);

    /**
     * Put a parameter in the form content. The form content type is "application/x-www-form-urlencoded".
     *
     * @param name  The parameter name.
     * @param value The parameter value.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder putFormParam(String name, String value);

    /**
     * Put a parameter in the form content. The form content type is "application/x-www-form-urlencoded".
     *
     * @param name   The parameter name.
     * @param values The parameter values.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder putFormParam(String name, List<String> values);

    /**
     * Remove a parameter in the form content. The form content type is "application/x-www-form-urlencoded".
     *
     * @param name The parameter name.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder removeFormParam(String name);

    /**
     * Add a value in an existed query parameter.
     *
     * @param name  The parameter name.
     * @param value The value.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder addQueryParam(String name, String value);

    /**
     * Add some values in an existed query parameter.
     *
     * @param name   The parameter name.
     * @param values The parameter values.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder addQueryParam(String name, List<String> values);

    /**
     * Put a parameter in the query parameter.
     *
     * @param name  The parameter name.
     * @param value The value.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder putQueryParam(String name, String value);

    /**
     * Put a parameter in the query parameter.
     *
     * @param name   The parameter name.
     * @param values The parameter values.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder putQueryParam(String name, List<String> values);

    /**
     * Remove a parameter in the query parameter.
     *
     * @param name The parameter name.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder removeQueryParam(String name);

    /**
     * Set the HTTP content receiving callback.
     *
     * @param contentHandler The HTTP content receiving callback. When the HTTP client receives the HTTP body data,
     *                       it will execute this action. This action will be executed many times.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder contentHandler(HttpClientContentHandler contentHandler);

    /**
     * Set the HTTP2 settings.
     *
     * @param http2Settings The HTTP2 settings.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder http2Settings(Map<Integer, Integer> http2Settings);

    /**
     * Submit the HTTP request.
     *
     * @return The HTTP response.
     */
    CompletableFuture<HttpClientResponse> submit();

}
