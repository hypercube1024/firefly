package com.fireflysource.common.concurrent

import com.fireflysource.common.sys.Result
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

fun <T> CompletionStage<T>.exceptionallyCompose(block: (Throwable) -> CompletionStage<T>): CompletableFuture<T> {
    return CompletableFutures.exceptionallyCompose(this) { block(it) }.toCompletableFuture()
}

fun <T> CompletionStage<T>.exceptionallyAccept(block: (Throwable) -> Unit): CompletableFuture<Void> {
    return this.exceptionally {
        block(it)
        null
    }.thenCompose { Result.DONE }.toCompletableFuture()
}