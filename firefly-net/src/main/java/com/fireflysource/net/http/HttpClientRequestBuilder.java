package com.fireflysource.net.http;

import com.fireflysource.net.http.model.Cookie;
import com.fireflysource.net.http.model.HttpField;
import com.fireflysource.net.http.model.HttpFields;
import com.fireflysource.net.http.model.HttpHeader;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Supplier;

/**
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
     * Put an HTTP field. It will replace existed field.
     *
     * @param name The field name.
     * @param list The field values.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder put(String name, List<String> list);

    /**
     * Put an HTTP field. It will replace existed field.
     *
     * @param header The field name.
     * @param value  The field value.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder put(HttpHeader header, String value);

    /**
     * Put an HTTP field. It will replace existed field.
     *
     * @param name  The field name.
     * @param value The field value.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder put(String name, String value);

    /**
     * Put an HTTP field. It will replace existed field.
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
     * Get the HTTP trailers.
     *
     * @return The HTTP trailers.
     */
    Supplier<HttpFields> getTrailerSupplier();

    /**
     * Set the HTTP trailers.
     *
     * @param trailers The HTTP trailers.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder setTrailerSupplier(Supplier<HttpFields> trailers);

    /**
     * Set the JSON HTTP body data.
     *
     * @param obj The JSON HTTP body data. The HTTP client will serialize the object when the request is submitted.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder jsonBody(Object obj);

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
     * Write HTTP body data. When you submit the request, the data will be sent.
     *
     * @param buffer The HTTP body data.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder write(ByteBuffer buffer);

    /**
     * Add a multi-part mime content. Such as a file.
     *
     * @param name    The content name.
     * @param content The ContentProvider that helps you read the content.
     * @param fields  The header fields of the content.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder addFieldPart(String name, ContentProvider content, HttpFields fields);

    /**
     * Add a multi-part mime content. Such as a file.
     *
     * @param name     The content name.
     * @param fileName The content file name.
     * @param content  The ContentProvider that helps you read the content.
     * @param fields   The header fields of the content.
     * @return RequestBuilder
     */
    HttpClientRequestBuilder addFilePart(String name, String fileName, ContentProvider content, HttpFields fields);

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
     * @param value The parameter value.
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
     * @param value The parameter value.
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
}
