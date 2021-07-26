package com.fireflysource.common.coroutine

import kotlinx.coroutines.channels.Channel

inline fun <T> Channel<T>.consumeAll(crossinline block: (T) -> Unit) {
    try {
        while (true) {
            val result = this.tryReceive()
            if (result.isFailure) {
                break
            }
            val message = result.getOrNull() ?: break
            block(message)
        }
    } catch (ignore: Exception) {
    }
}

fun <T> Channel<T>.clear() {
    this.consumeAll { }
}