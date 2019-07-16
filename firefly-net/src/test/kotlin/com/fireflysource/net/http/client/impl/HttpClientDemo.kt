package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.utils.LifeCycleUtils.stopAll
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val httpClient = HttpClientFactory.create()

    val response = httpClient.get("https://www.baidu.com").submit().await()
    println("${response.status} ${response.reason}")
    println(response.httpFields)
    println(response.stringBody)
    println()

    stopAll()
}