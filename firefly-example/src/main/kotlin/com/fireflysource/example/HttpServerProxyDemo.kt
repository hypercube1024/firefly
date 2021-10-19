package com.fireflysource.example

import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.server.impl.HttpProxy
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await

suspend fun main() {
    val proxy = HttpProxy()
    proxy.listen("localhost", 1678)

    val client = createProxyHttpClient("localhost", 1678)
    testHttpsProxy(client)
    delay(1000)
    testHttpProxy(client)

}

suspend fun testHttpsProxy(client: HttpClient) {
    val response = client.get("https://www.baidu.com/").submit().await()
    printResponse(response)
}

suspend fun testHttpProxy(client: HttpClient) {
    val response = client.get("http://www.fireflysource.com/").submit().await()
    printResponse(response)
}

private fun printResponse(response: HttpClientResponse) {
    println("${response.status} ${response.reason}")
    println(response.httpFields)
    println(response.stringBody)
    println("-------------------------------------------")
    println()
}