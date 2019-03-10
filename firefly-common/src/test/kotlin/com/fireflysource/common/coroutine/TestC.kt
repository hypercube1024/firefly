package com.fireflysource.common.coroutine

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // this: CoroutineScope
    coroutineScope {
        delay(200L)
        println("Task from runBlocking")
    }

    coroutineScope {
        delay(100L)
        println("Task from coroutine scope")
    }

    println("Coroutine scope is over")
}