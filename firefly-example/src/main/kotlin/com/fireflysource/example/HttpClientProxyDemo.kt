package com.fireflysource.example

import com.fireflysource.fx
import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.ProxyConfig
import kotlinx.coroutines.future.await

suspend fun main() {
    val client = createProxyHttpClient("127.0.0.1", 1091)
//    val host = "nghttp2.org"
    val host = "www.google.com"
    val response = client
        .get("https://$host/")
        .submit().await()
    println("${response.status} ${response.reason}")
    println(response.httpFields)
    println(response.stringBody)
}

fun createProxyHttpClient(host: String, port: Int): HttpClient {
    val proxyConfig = ProxyConfig()
    proxyConfig.host = host
    proxyConfig.port = port
    val httpConfig = HttpConfig()
    httpConfig.proxyConfig = proxyConfig
    return fx.createHttpClient(httpConfig)
}