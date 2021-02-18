package com.fireflysource.net.http.common.v1.decoder

import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer

suspend fun HttpParser.parseAll(tcpConnection: TcpConnection): ByteBuffer? {
    var lastBuffer: ByteBuffer? = null
    readLoop@ while (!isState(HttpParser.State.END)) {
        val buffer = tcpConnection.read().await()
        lastBuffer = buffer
        parseLoop@ while (buffer.remaining() > 0) {
            val beforeRemaining = buffer.remaining()
            val exit = this.parseNext(buffer)
            val afterRemaining = buffer.remaining()
            when {
                exit -> break@readLoop
                isState(HttpParser.State.END) -> break@readLoop
                beforeRemaining == afterRemaining -> throw BadMessageException("The received data cannot be consumed")
            }
        }
    }
    return lastBuffer
}