package com.fireflysource.net.http.common.v1.decoder

import com.fireflysource.common.`object`.Assert
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer
import java.util.function.Predicate

suspend fun HttpParser.parseAll(tcpConnection: TcpConnection): ByteBuffer? {
    return this.parse(tcpConnection, Predicate { it == HttpParser.State.END })
}

suspend fun HttpParser.parse(tcpConnection: TcpConnection, terminal: Predicate<HttpParser.State>): ByteBuffer? {
    fun isTerminal() = terminal.test(this.state) || this.isState(HttpParser.State.END)

    var byteBuffer: ByteBuffer? = null
    recvLoop@ while (!isTerminal()) {
        val buffer = try {
            tcpConnection.read().await()
        } catch (e: Exception) {
            break@recvLoop
        }
        byteBuffer = buffer

        var remaining = buffer.remaining()
        while (!isTerminal() && remaining > 0) {
            val wasRemaining = remaining
            val end = this.parseNext(buffer)
            if (end) break@recvLoop

            remaining = buffer.remaining()
            Assert.state(remaining != wasRemaining, "The received data cannot be consumed")
        }
    }
    return byteBuffer
}