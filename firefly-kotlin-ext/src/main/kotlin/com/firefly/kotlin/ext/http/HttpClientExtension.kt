package com.firefly.kotlin.ext.http

import com.firefly.client.http2.SimpleHTTPClient
import com.firefly.client.http2.SimpleResponse
import com.firefly.codec.http2.model.HttpFields
import com.firefly.kotlin.ext.common.Json
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

/**
 * Firefly HTTP client extensions
 *
 * @author Pengtao Qiu
 */

inline fun <reified T : Any> SimpleResponse.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> SimpleResponse.getJsonBody(): T = Json.parse(stringBody)

suspend fun SimpleHTTPClient.RequestBuilder.asyncSubmit(
    time: Long = 60 * 1000L,
    unit: TimeUnit = TimeUnit.MILLISECONDS
                                                       ): SimpleResponse = withTimeout(unit.toMillis(time)) { submit().await() }

fun SimpleResponse.getTrailer(): HttpFields = trailerSupplier.get()