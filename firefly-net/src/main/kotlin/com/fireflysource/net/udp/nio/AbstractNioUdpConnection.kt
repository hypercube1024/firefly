package com.fireflysource.net.udp.nio

import com.fireflysource.common.coroutine.consumeAll
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.AbstractConnection
import com.fireflysource.net.udp.UdpConnection
import com.fireflysource.net.udp.UdpCoroutineDispatcher
import com.fireflysource.net.udp.buffer.CancelSelectionKey
import com.fireflysource.net.udp.buffer.InputBuffer
import com.fireflysource.net.udp.buffer.InputMessage
import com.fireflysource.net.udp.buffer.InvalidSelectionKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.nio.channels.ClosedChannelException
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

abstract class AbstractNioUdpConnection(
    id: Int,
    maxIdleTime: Long,
    dispatcher: CoroutineDispatcher,
    inputBufferSize: Int,
    private val nioUdpCoroutineDispatcher: UdpCoroutineDispatcher = NioUdpCoroutineDispatcher(id, dispatcher),
    private val datagramChannel: DatagramChannel,
) : AbstractConnection(id, System.currentTimeMillis(), maxIdleTime), UdpConnection,
    UdpCoroutineDispatcher by nioUdpCoroutineDispatcher {

    companion object {
        private val log = SystemLogger.create(AbstractNioUdpConnection::class.java)
        private val timeUnit = TimeUnit.SECONDS
    }

    private val closeResultChannel: Channel<Consumer<Result<Void>>> = Channel(Channel.UNLIMITED)
    private val inputMessageHandler = InputMessageHandler(inputBufferSize)

    private inner class InputMessageHandler(inputBufferSize: Int) {
        private val inputMessageChannel: Channel<InputMessage> = Channel(Channel.UNLIMITED)
        private val inputBuffer = BufferUtils.allocateDirect(inputBufferSize)
        private var readTimeout = maxIdleTime


        private fun readJob() = coroutineScope.launch {
            while (true) {
                handleInputMessage(inputMessageChannel.receive())
            }
        }.invokeOnCompletion { cause ->
            val e = cause ?: ClosedChannelException()
            inputMessageChannel.consumeAll { message ->
                if (message is InputBuffer) {
                    message.bufferFuture.completeExceptionally(e)
                }
            }
            closeResultChannel.consumeAll { it.accept(Result.SUCCESS) }
        }

        private suspend fun handleInputMessage(input: InputMessage) {
            when {
                input is InputBuffer -> {

                }
                input is CancelSelectionKey -> {

                }
                input is InvalidSelectionKey -> {

                }
            }
        }

        fun sendInvalidSelectionKeyMessage() {
            val result = inputMessageChannel.trySend(InvalidSelectionKey)
            if (result.isFailure) {
                log.error { "send invalid selection key message failure" }
            }
        }

        fun readComplete() {
            val pos = inputBuffer.flipToFill()
            val result = runCatching { datagramChannel.read(inputBuffer) }
            inputBuffer.flipToFlush(pos)
            // TODO
        }
    }

    fun sendInvalidSelectionKeyMessage() {
        inputMessageHandler.sendInvalidSelectionKeyMessage()
    }

    fun readComplete(): ReadResult {
        return ReadResult.CONTINUE_READ
    }

    fun writeComplete(): WriteResult {
        return WriteResult.CONTINUE_WRITE
    }
}

enum class ReadResult {
    REMOTE_CLOSE, SUSPEND_READ, CONTINUE_READ
}

enum class WriteResult {
    REMOTE_CLOSE, SUSPEND_WRITE, CONTINUE_WRITE
}