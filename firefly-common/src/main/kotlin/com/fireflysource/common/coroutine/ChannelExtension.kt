package com.fireflysource.common.coroutine

import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

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
    } finally {
        this.close()
    }
}

fun <T> Channel<T>.clear() {
    this.consumeAll { }
}

class Signal<T> {
    private val channel: Channel<T> = Channel(1)
    private val notified = AtomicBoolean(false)

    suspend fun wait(): T = channel.receive()

    fun notify(e: T) {
        if (notified.compareAndSet(false, true)) {
            channel.trySend(e)
        }
    }

    fun reset() {
        notified.set(false)
    }
}