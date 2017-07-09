package com.firefly.kotlin.ext.example

import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.asyncSubmit
import com.firefly.kotlin.ext.http.getJsonBody
import com.firefly.kotlin.ext.log.Log
import com.firefly.kotlin.ext.log.info
import kotlinx.coroutines.experimental.runBlocking

/**
 * HTTP client asynchronous wait example
 *
 * @author Pengtao Qiu
 */

private val log = Log.getLogger { }

private val host = "http://localhost:8080"

fun main(args: Array<String>): Unit = runBlocking {
    val hello = firefly.httpClient().get(host).asyncSubmit().stringBody
    log.info { "hello -> $hello" }

    val msg = firefly.httpClient().get("$host/jsonMsg").asyncSubmit().getJsonBody<Response<String>>()
    log.info { "json msg -> $msg" }

    val fuck = firefly.httpClient().get("$host/product/fuck/3").asyncSubmit()
    log.info { "product -> ${fuck.trailerSupplier.get()["You-are-trailer"]}" }

    val duck = firefly.httpClient().post("$host/product/duck/33").asyncSubmit().getJsonBody<Response<Product>>()
    log.info { "duck -> $duck" }

    val postResponse = firefly.httpClient().post("$host/product")
            .jsonBody(Request("mmp product", Product("Oooo", "mmp")))
            .asyncSubmit().getJsonBody<Response<String>>()
    log.info { "mmp -> $postResponse" }

    val getResponse = firefly.httpClient().get("$host/product")
            .jsonBody(Request("mmp get product", Product("Error", "mmp")))
            .asyncSubmit().stringBody
    log.info { "error -> $getResponse" }

}