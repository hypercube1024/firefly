package com.fireflysource.net.http.common.v1.decoder

import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer

suspend fun HttpParser.parseAll(tcpConnection: TcpConnection): ByteBuffer? {
    var lastBuffer: ByteBuffer? = null
    recvLoop@ while (!this.isState(HttpParser.State.END)) {
        val buffer = tcpConnection.read().await()
        lastBuffer = buffer
        parseLoop@ while (!this.isState(HttpParser.State.END) && buffer.remaining() > 0) {
            val remainingBeforeParsing = buffer.remaining()
            val exit = this.parseNext(buffer)
            val remainingAfterParsing = buffer.remaining()
            if (remainingBeforeParsing == remainingAfterParsing) {
                throw BadMessageException("The received data cannot be consumed")
            }
            if (exit) {
                break@recvLoop
            }
        }
    }
    return lastBuffer
}