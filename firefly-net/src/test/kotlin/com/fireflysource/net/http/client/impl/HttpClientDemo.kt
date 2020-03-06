package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientFactory
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val httpClient = HttpClientFactory.create()
    val response = httpClient.get("http://nghttp2.org")
        .upgradeHttp2()
        .submit().await()
    println("${response.status} ${response.reason}")
    println(response.httpFields)
    println(response.stringBody)
    println()
    httpClient.stop()

//    AbstractLifeCycle.stopAll()
//    CoroutineDispatchers.stopAll()
}