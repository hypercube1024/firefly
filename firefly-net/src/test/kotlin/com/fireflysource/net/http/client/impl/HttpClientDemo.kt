package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.CoroutineDispatchers
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.net.http.client.HttpClientFactory
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    // http://localhost:8080/hello-world
    // http://nghttp2.org
    val httpClient = HttpClientFactory.create()

    val count = 20
    (1..count).forEach {
        val response = httpClient.get("http://localhost:8080/hello-world")
            .putQueryString("name", "PT_$it")
            .upgradeHttp2()
            .submit().await()
        println("${response.status} ${response.reason}")
        println(response.httpFields)
        println(response.stringBody)
        println()
    }

    httpClient.stop()

    AbstractLifeCycle.stopAll()
    CoroutineDispatchers.stopAll()
}