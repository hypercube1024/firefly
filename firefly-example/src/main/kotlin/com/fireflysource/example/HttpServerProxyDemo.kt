package com.fireflysource.example

import com.fireflysource.net.http.server.impl.HttpProxy
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await

suspend fun main() {
    val proxy = HttpProxy()
    proxy.listen("localhost", 1678)

    val client = createProxyHttpClient("localhost", 1678)

    repeat(1) {
        val response = client.get("https://www.baidu.com/").submit().await()
        println("${response.status} ${response.reason}")
        println(response.httpFields)
        println(response.stringBody.length)
        println("-------------------------------------------")
        println()
        delay(2000)
    }

    val response = client.get("http://www.fireflysource.com/").submit().await()
    println("${response.status} ${response.reason}")
    println(response.httpFields)
    println(response.stringBody)
}