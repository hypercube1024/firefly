package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.net.http.server.impl.router.handler.CorsConfig
import com.fireflysource.net.http.server.impl.router.handler.CorsHandler

fun main() {
    val corsConfig = CorsConfig("*.cors.test.com")
    `$`.httpServer()
        .router().path("*").handler(CorsHandler(corsConfig))
        .router().post("/cors-data-request/*")
        .handler { it.end("success") }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().post("$url/cors-data-request/xxx")
        .put(HttpHeader.ORIGIN, "hello.cors.test.com")
        .put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN_UTF_8.value)
        .body("hello")
        .submit().thenAccept { response -> println(response) }
}