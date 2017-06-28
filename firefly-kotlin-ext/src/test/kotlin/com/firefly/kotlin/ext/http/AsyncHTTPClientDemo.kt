package com.firefly.kotlin.ext.http

import com.firefly.kotlin.ext.common.firefly
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.runBlocking

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>): Unit = runBlocking {
    val response = firefly.httpClient().get("http://localhost:8080/jsonMsg").submit().await()
    val msg: JsonMsg = response.getJsonBody()
    println("msg: ${msg.code}, ${msg.msg}")
}