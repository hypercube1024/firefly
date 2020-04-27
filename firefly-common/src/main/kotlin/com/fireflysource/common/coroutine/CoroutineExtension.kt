package com.fireflysource.common.coroutine

import com.fireflysource.common.sys.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.CompletableFuture

fun Job.asVoidFuture(): CompletableFuture<Void> {
    return this.asCompletableFuture().thenCompose { Result.DONE }
}