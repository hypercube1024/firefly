package com.firefly.example.kotlin.test.coroutine

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) = runBlocking(CoroutineName("main")) {
    GlobalScope.launch(CoroutineName("test1")) {
        println("Running in ${coroutineContext[CoroutineName]}")
    }.join()
    println("Running in ${coroutineContext[CoroutineName]}")
    Unit
}