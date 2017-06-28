package com.firefly.kotlin.ext.http

import com.firefly.client.http2.SimpleResponse
import com.firefly.kotlin.ext.common.AsyncPool
import com.firefly.kotlin.ext.common.Json
import com.firefly.server.http2.HTTP2ServerBuilder
import com.firefly.server.http2.SimpleRequest
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.experimental.launch

/**
 * @author Pengtao Qiu
 */
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

inline fun <reified T : Any> SimpleRequest.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> SimpleRequest.getJsonBody(): T = Json.parse(stringBody)

inline fun <reified T : Any> SimpleResponse.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> SimpleResponse.getJsonBody(): T = Json.parse(stringBody)