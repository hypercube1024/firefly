package com.fireflysource.net.http.common.v1.decoder

import com.fireflysource.common.`object`.Assert
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.future.await
import java.util.function.Predicate

suspend fun HttpParser.parseAll(tcpConnection: TcpConnection) {
    this.parse(tcpConnection, Predicate { it == HttpParser.State.END })
}

suspend fun HttpParser.parse(tcpConnection: TcpConnection, terminal: Predicate<HttpParser.State>) {
    fun isTerminal() = terminal.test(this.state) || this.isState(HttpParser.State.END)

    recvLoop@ while (!isTerminal()) {
        val buffer = try {
            tcpConnection.read().await()
        } catch (e: Exception) {
            break@recvLoop
        }

        var remaining = buffer.remaining()
        while (!isTerminal() && remaining > 0) {
            val wasRemaining = remaining
            this.parseNext(buffer)
            remaining = buffer.remaining()
            Assert.state(remaining != wasRemaining, "The received data cannot be consumed")
        }
    }
}