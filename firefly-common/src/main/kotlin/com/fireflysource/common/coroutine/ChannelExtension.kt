package com.fireflysource.common.coroutine

import kotlinx.coroutines.channels.Channel

inline fun <T> Channel<T>.pollAll(crossinline block: (T) -> Unit) {
    while (true) {
        val message = this.poll()
        if (message != null) block(message) else break
    }
}