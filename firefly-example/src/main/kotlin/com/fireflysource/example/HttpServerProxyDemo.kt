package com.fireflysource.example

import com.fireflysource.net.http.server.impl.HttpProxy
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await

suspend fun main() {
    val proxy = HttpProxy()
    proxy.listen("localhost", 1678)

    val client = createProxyHttpClient("localhost", 1678)

    repeat(3) {
        val response = client
            .get("https://www.baidu.com/")
            .submit().await()
        println("${response.status} ${response.reason}")
        println(response.httpFields)
        println(response.stringBody.length)
        delay(2000)
    }
}