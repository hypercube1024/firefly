package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.CoroutineDispatchers
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.f
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking


fun main(): Unit = runBlocking {
    // http://localhost:8080/hello-world
    // http://nghttp2.org

    val count = 1
    (1..count).forEach {
        val response = f.httpClient()
            .get("http://nghttp2.org")
            .putQueryString("name", "PT_$it")
            .upgradeHttp2()
            .submit().await()
        println("${response.status} ${response.reason}")
        println(response.httpFields)
        println(response.stringBody)
        println()
    }

    AbstractLifeCycle.stopAll()
    CoroutineDispatchers.stopAll()
}