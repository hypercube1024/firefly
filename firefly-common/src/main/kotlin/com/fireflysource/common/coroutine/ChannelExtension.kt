package com.fireflysource.common.coroutine

import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

inline fun <T> Channel<T>.pollAll(crossinline block: (T) -> Unit) {
    while (true) {
        val message = this.poll()
        if (message != null) block(message) else break
    }
}

class Signal<T> {
    private val channel: Channel<T> = Channel(Channel.UNLIMITED)
    private val notified = AtomicBoolean(false)

    suspend fun wait(): T = channel.receive()

    fun notify(e: T) {
        if (notified.compareAndSet(false, true)) {
            channel.offer(e)
        }
    }

    fun reset() {
        notified.set(false)
        channel.pollAll { }
    }
}