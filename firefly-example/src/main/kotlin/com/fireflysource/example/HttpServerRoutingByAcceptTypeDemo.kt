package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.serialization.SerializationServiceFactory.json

fun main() {
    `$`.httpServer()
        .router().get("/product/:id").produces("text/plain")
        .handler { ctx ->
            ctx.end(Car("Benz", "Black").toString())
        }
        .router().get("/product/:id").produces("application/json")
        .handler { ctx ->
            ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.value)
                .end(json().write(Car("Benz", "Black")))
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().get("$url/product/3")
        .put(HttpHeader.ACCEPT, "text/plain, application/json;q=0.9, */*;q=0.8")
        .submit().thenAccept { response -> println("accept text; ${response.stringBody}") }

    `$`.httpClient().get("$url/product/3")
        .put(HttpHeader.ACCEPT, "application/json, text/plain, */*;q=0.8")
        .submit().thenAccept { response -> println("accept json; ${response.stringBody}") }
}