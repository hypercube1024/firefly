package com.firefly.example.kotlin.test.coroutine

import com.firefly.kotlin.ext.common.CoroutineDispatchers.computation
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) = runBlocking(CoroutineName("main")) {
    testThreadLocal()
}

fun testContext1() = runBlocking(CoroutineName("testContext1")) {
    GlobalScope.launch(CoroutineName("test1")) {
        println("Running in ${coroutineContext[CoroutineName]}")
    }.join()
    println("Running in ${coroutineContext[CoroutineName]}")
    Unit
}

val threadLocal = ThreadLocal<String>()
suspend fun testThreadLocal() {
    GlobalScope.launch(
        CoroutineName("test1")
                + threadLocal.asContextElement("localTest1")
                + computation
                      ) {
        println("Running in launch block -> ${coroutineContext[CoroutineName]}, ${Thread.currentThread().name}, ${threadLocal.get()}")
        testExtractFun()
    }.join()
}

suspend fun testExtractFun() {
    println("Running in testExtractFun -> ${coroutineContext[CoroutineName]}, ${Thread.currentThread().name}, ${threadLocal.get()}")
}