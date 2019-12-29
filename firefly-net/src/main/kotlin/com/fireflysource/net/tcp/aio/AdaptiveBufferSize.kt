package com.fireflysource.net.tcp.aio

/**
 * @author Pengtao Qiu
 */
class AdaptiveBufferSize {
    companion object {
        private val sizeArray = arrayOf(
            1 * 1024,
            2 * 1024,
            4 * 1024,
            4 * 1024,
            8 * 1024,
            8 * 1024,
            8 * 1024,
            16 * 1024,
            16 * 1024,
            16 * 1024,
            32 * 1024,
            32 * 1024,
            64 * 1024,
            128 * 1024,
            256 * 1024,
            512 * 1024
        )
    }

    private var index: Int = 0

    fun getBufferSize() = sizeArray[index]

    fun update(size: Int) {
        index = if (size >= getBufferSize()) {
            (index + 1).coerceAtMost(sizeArray.lastIndex)
        } else {
            (index - 1).coerceAtLeast(0)
        }
    }
}