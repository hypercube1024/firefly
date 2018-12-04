package com.fireflysource.net.tcp.aio

/**
 * @author Pengtao Qiu
 */
class AdaptiveBufferSize {
    companion object {
        private val sizeArray = arrayOf(
            1024,
            2 * 1024,
            4 * 1024,
            8 * 1024,
            16 * 1024,
            32 * 1024,
            64 * 1024
                                       )
    }

    private var index: Int = 0

    fun getBufferSize() = sizeArray[index]

    fun setCurrentDataSize(size: Int) {
        index = if (size >= getBufferSize()) {
            Math.min(index + 1, sizeArray.lastIndex)
        } else {
            Math.max(index - 1, 0)
        }
    }
}