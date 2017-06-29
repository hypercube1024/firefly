package com.firefly.kotlin.ext.http

import com.firefly.client.http2.SimpleHTTPClient
import com.firefly.client.http2.SimpleResponse
import com.firefly.kotlin.ext.common.Json
import kotlinx.coroutines.experimental.future.await

/**
 * Firefly HTTP client extensions
 *
 * @author Pengtao Qiu
 */

inline fun <reified T : Any> SimpleResponse.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> SimpleResponse.getJsonBody(): T = Json.parse(stringBody)

suspend fun SimpleHTTPClient.RequestBuilder.asyncSubmit(): SimpleResponse = submit().await()