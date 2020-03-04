package com.fireflysource.common.concurrent

import com.fireflysource.common.sys.Result
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

inline fun <T> CompletionStage<T>.exceptionallyCompose(crossinline block: (Throwable) -> CompletionStage<T>): CompletableFuture<T> {
    return CompletableFutures.exceptionallyCompose(this) { block(it) }.toCompletableFuture()
}

inline fun <T> CompletionStage<T>.exceptionallyAccept(crossinline block: (Throwable) -> Unit): CompletableFuture<Void> {
    return this.exceptionally {
        block(it)
        null
    }.thenCompose { Result.DONE }.toCompletableFuture()
}