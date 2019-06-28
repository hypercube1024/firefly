package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpURI
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.net.URL

fun main() = runBlocking {
    val client = AsyncHttpClientConnectionManager()

    val request = AsyncHttpClientRequest()
    request.method = HttpMethod.GET.value
    request.uri = HttpURI(URL("https://www.baidu.com:443/").toURI())

    request.httpFields.put(
        HttpHeader.USER_AGENT,
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36"
    )
    val response = client.send(request).await()
    println("${response.status} ${response.reason}")
    println(response.httpFields)
    println()
    println(response.stringBody)

    println("exit.")
}