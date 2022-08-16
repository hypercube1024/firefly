package com.fireflysource.net.udp.nio

import com.fireflysource.common.coroutine.consumeAll
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.copy
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.AbstractConnection
import com.fireflysource.net.udp.UdpConnection
import com.fireflysource.net.udp.UdpCoroutineDispatcher
import com.fireflysource.net.udp.buffer.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.jctools.queues.SpscLinkedQueue
import java.nio.ByteBuffer
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
    private val nioUdpWorker: NioUdpWorker,
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
        private val readRequestQueue = SpscLinkedQueue<InputBuffer>()
        private val readCompleteQueue = SpscLinkedQueue<ByteBuffer>()
        private var readTimeout = maxIdleTime
        private var registeredRead = false
        private var readWaterline = 0


        private fun readJob() = coroutineScope.launch {
            while (true) {
                when (val msg = inputMessageChannel.receive()) {
                    is InputBuffer -> {
                        readRequestQueue.offer(msg)
                        if (!registeredRead) {
                            nioUdpWorker.registerRead(datagramChannel, this@AbstractNioUdpConnection).await()
                            registeredRead = true
                        }
                    }

                    is CancelSelectionKey -> {

                    }

                    is InvalidSelectionKey -> {

                    }

                    is ReadComplete -> {

                    }
                }
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

        fun sendInvalidSelectionKeyMessage() {
            val result = inputMessageChannel.trySend(InvalidSelectionKey)
            if (result.isFailure) {
                log.error { "send invalid selection key message failure" }
            }
        }

        fun readComplete(): ReadResult {
            val pos = inputBuffer.flipToFill()
            val result = runCatching { datagramChannel.read(inputBuffer) }
            inputBuffer.flipToFlush(pos)

            return if (result.isSuccess) {
                if (result.getOrDefault(0) >= 0) {
                    val input = readRequestQueue.poll()
                    if (input != null) {
                        val buffer = readCompleteQueue.poll()
                        if (buffer != null) {
                            readWaterline -= buffer.remaining()
                            input.bufferFuture.complete(buffer)
                            inputBuffer.copy().also {
                                readWaterline += it.remaining()
                                readCompleteQueue.offer(it)
                            }
                        } else {
                            input.bufferFuture.complete(inputBuffer.copy())
                        }
                    } else {
                        inputBuffer.copy().also {
                            readWaterline += it.remaining()
                            readCompleteQueue.offer(it)
                        }
                    }
                    ReadResult.CONTINUE_READ
                } else {
                    ReadResult.REMOTE_CLOSE
                }
            } else {
                ReadResult.CONTINUE_READ
            }
        }
    }

    fun sendInvalidSelectionKeyMessage() {
        inputMessageHandler.sendInvalidSelectionKeyMessage()
    }

    fun readComplete(): ReadResult {
        return inputMessageHandler.readComplete()
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