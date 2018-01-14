package com.firefly.kotlin.ext.http

import com.firefly.utils.concurrent.ThreadUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

/**
 * @author Pengtao Qiu
 */
val service = Executors.newCachedThreadPool()

fun main(args: Array<String>) {
    testUnconfined()
    testCommonPool()
    testBlocking()
}

fun testBlocking() = runBlocking {
    println(Thread.currentThread().name + ". blocking start")
    val ret = CompletableFuture.supplyAsync(Supplier<String> {
        ThreadUtils.sleep(5, TimeUnit.SECONDS)
        "OK"
    }, service).await()
    println(Thread.currentThread().name + ". blocking $ret")
}

fun testUnconfined() {
    launch(Unconfined) {
        println(Thread.currentThread().name + ". unconfined start")
        val ret = CompletableFuture.supplyAsync(Supplier<String> {
            ThreadUtils.sleep(2, TimeUnit.SECONDS)
            "OK"
        }, service).await()
        println(Thread.currentThread().name + ". unconfined $ret")
    }
}

fun testCommonPool() {
    launch(CommonPool) {
        println(Thread.currentThread().name + ". common start")
        val ret = CompletableFuture.supplyAsync(Supplier<String> {
            ThreadUtils.sleep(2, TimeUnit.SECONDS)
            "OK"
        }, service).await()
        println(Thread.currentThread().name + ". common $ret")
    }
}