package com.firefly.kotlin.ext.http

import com.firefly.client.http2.SimpleHTTPClient
import com.firefly.client.http2.SimpleResponse
import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.kotlin.ext.common.AsyncPool
import com.firefly.kotlin.ext.common.Json
import com.firefly.server.http2.HTTP2ServerBuilder
import com.firefly.server.http2.SimpleRequest
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.launch

/**
 * @author Pengtao Qiu
 */

// HTTP server extension

fun HTTP2ServerBuilder.asyncHandler(handler: suspend RoutingContext.() -> Unit): HTTP2ServerBuilder {
    this.handler {
        it.response.isAsynchronous = true
        launch(AsyncPool) {
            handler.invoke(it)
        }
    }
    return this
}

inline fun <reified T : Any> RoutingContext.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> RoutingContext.getJsonBody(): T = Json.parse(stringBody)

fun RoutingContext.writeJson(obj: Any): RoutingContext = put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON.asString()).write(Json.toJson(obj))

inline fun <reified T : Any> SimpleRequest.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> SimpleRequest.getJsonBody(): T = Json.parse(stringBody)


// HTTP client extension

inline fun <reified T : Any> SimpleResponse.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> SimpleResponse.getJsonBody(): T = Json.parse(stringBody)

suspend fun SimpleHTTPClient.RequestBuilder.asyncSubmit(): SimpleResponse = submit().await()