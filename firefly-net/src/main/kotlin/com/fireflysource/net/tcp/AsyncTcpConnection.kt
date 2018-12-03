package com.fireflysource.net.tcp

import com.fireflysource.net.tcp.TcpConnection.DEFAULT_CHARSET
import java.nio.ByteBuffer

/**
 * @author Pengtao Qiu
 */
interface AsyncTcpConnection : TcpConnection {

    suspend fun asyncWrite(byteBuffer: ByteBuffer)

    suspend fun asyncWrite(byteBuffers: Array<ByteBuffer>)

    suspend fun asyncWrite(data: ByteArray) {
        asyncWrite(ByteBuffer.wrap(data))
    }

    suspend fun asyncWrite(data: String) {
        asyncWrite(ByteBuffer.wrap(data.toByteArray(DEFAULT_CHARSET)))
    }

    suspend fun asyncWrite(byteBuffers: Collection<ByteBuffer>) {
        asyncWrite(byteBuffers.toTypedArray())
    }
}