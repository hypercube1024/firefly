package com.fireflysource.example

import com.fireflysource.net.http.server.impl.HttpProxy
import kotlinx.coroutines.future.await

suspend fun main() {
    val proxy = HttpProxy()
    proxy.listen("localhost", 1678)

    val client = createProxyHttpClient("localhost", 1678)
    val host = "www.baidu.com"
    val response = client
        .get("https://$host/")
        .submit().await()
    println("${response.status} ${response.reason}")
    println(response.httpFields)
    println(response.stringBody)
}