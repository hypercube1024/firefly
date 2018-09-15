package com.firefly.example.kotlin.test.coroutine

import kotlinx.coroutines.experimental.*

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) = runBlocking {
    launchDoWorld()
    println("Hello,")
}

// this is your first suspending function
suspend fun launchDoWorld() = currentScope {
    launch {
        println("World!")
    }
}

fun hello2() = runBlocking {
    launch { doWorld() }
    println("Hello,")
}

// this is your first suspending function
suspend fun doWorld() {
    delay(1000L)
    println("World!")
}

fun hello1() = runBlocking {
    // this: CoroutineScope
    launch {
        // launch new coroutine in the scope of runBlocking
        delay(1000L)
        println("World!")
    }
    println("Hello,")
}


fun test() = runBlocking {
    // this: CoroutineScope
    launch {
        delay(200L)
        println("Task from runBlocking")
    }

    coroutineScope {
        // Creates a new coroutine scope
        launch {
            delay(500L)
            println("Task from nested launch")
        }

        delay(100L)
        println("Task from coroutine scope") // This line will be printed before nested launch
    }

    println("Coroutine scope is over") // This line is not printed until nested launch completes
}

