package com.fireflysource.common.concurrent

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

fun <T> CompletionStage<T>.exceptionallyCompose(block: (Throwable) -> CompletionStage<T>): CompletableFuture<T> {
    return CompletableFutures.exceptionallyCompose(this) { block(it) }.toCompletableFuture()
}

fun <T> CompletionStage<T>.exceptionallyAccept(block: (Throwable) -> Unit): CompletableFuture<T> {
    return this.exceptionally {
        block(it)
        null
    }.toCompletableFuture()
}