package com.fireflysource.net.http.common.v1.decoder

import com.fireflysource.common.`object`.Assert
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.future.await

suspend fun HttpParser.parseAll(tcpConnection: TcpConnection) {
    this.parse(tcpConnection, HttpParser.State.END)
}

suspend fun HttpParser.parse(tcpConnection: TcpConnection, terminalState: HttpParser.State) {
    Assert.state(this.isState(HttpParser.State.START), "The parser state error. ${this.state}")

    recvLoop@ while (!this.isState(terminalState)) {
        val buffer = try {
            tcpConnection.read().await()
        } catch (e: Exception) {
            break@recvLoop
        }

        var remaining = buffer.remaining()
        while (!this.isState(terminalState) && remaining > 0) {
            val wasRemaining = remaining
            this.parseNext(buffer)
            remaining = buffer.remaining()
            Assert.state(remaining != wasRemaining, "The received data cannot be consumed")
        }
    }
}