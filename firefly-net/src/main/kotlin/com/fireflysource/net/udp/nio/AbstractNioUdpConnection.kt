package com.fireflysource.net.udp.nio

import com.fireflysource.common.coroutine.consumeAll
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
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

abstract class AbstractNioUdpConnection(
    id: Int,
    maxIdleTime: Long,
    dispatcher: CoroutineDispatcher,
    inputBufferSize: Int,
    private val nioUdpCoroutineDispatcher: UdpCoroutineDispatcher = NioUdpCoroutineDispatcher(id, dispatcher),
    val datagramChannel: DatagramChannel,
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
        private val readRequestQueue = LinkedList<InputBuffer>()
        private val readCompleteQueue = LinkedList<ByteBuffer>()
        private var readTimeout = maxIdleTime
        private var readWaterline = 0
        private var selectionKey: SelectionKey? = null

        init {
            readJob()
        }


        private fun readJob() = coroutineScope.launch {
            selectionKey = nioUdpWorker.registerRead(this@AbstractNioUdpConnection).await()
            while (true) {
                when (val msg = inputMessageChannel.receive()) {
                    is InputBuffer -> offerReadRequest(msg)
                    is ReadComplete -> offerReadComplete(msg.buffer)
                    is CancelSelectionKey -> {
                        selectionKey = null
                    }
                    is InvalidSelectionKey -> {
                        selectionKey = null
                    }
                    is UnregisterRead -> {

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

        private fun offerReadComplete(buffer: ByteBuffer) {
            if (readCompleteQueue.offer(buffer)) {
                readWaterline += buffer.remaining()
                updateReadQueue()
            }
        }

        private fun pollReadComplete(): ByteBuffer? {
            val buffer = readCompleteQueue.poll()
            if (buffer != null) {
                readWaterline -= buffer.remaining()
            }
            return buffer
        }

        private fun offerReadRequest(request: InputBuffer) {
            if (readRequestQueue.offer(request)) {
                updateReadQueue()
            }
        }

        private fun updateReadQueue() {
            while (!readRequestQueue.isEmpty() && !readCompleteQueue.isEmpty()) {
                val readRequest = readRequestQueue.poll()!!
                val buffer: ByteBuffer = pollReadComplete()!!
                readRequest.bufferFuture.complete(buffer)
            }
        }

        fun sendInvalidSelectionKeyMessage() {
            val result = inputMessageChannel.trySend(InvalidSelectionKey)
            if (result.isFailure) {
                log.error { "send InvalidSelectionKey message failure" }
            }
        }

        fun sendCancelSelectionKeyMessage() {
            val result = inputMessageChannel.trySend(CancelSelectionKey)
            if (result.isFailure) {
                log.error { "send CancelSelectionKey message failure" }
            }
        }

        fun sendUnregisterReadMessage() {
            val result = inputMessageChannel.trySend(UnregisterRead)
            if (result.isFailure) {
                log.error { "send UnregisterRead message failure" }
            }
        }

        fun sendReadCompleteMessage(buffer: ByteBuffer) {
            val result = inputMessageChannel.trySend(ReadComplete(buffer))
            if (result.isFailure) {
                log.error { "send ReadComplete message failure" }
            }
        }

    }

    fun sendInvalidSelectionKeyMessage() {
        inputMessageHandler.sendInvalidSelectionKeyMessage()
    }

    fun sendCancelSelectionKeyMessage() {
        inputMessageHandler.sendCancelSelectionKeyMessage()
    }

    fun sendUnregisterReadMessage() {
        inputMessageHandler.sendUnregisterReadMessage()
    }

    fun sendReadCompleteMessage(buffer: ByteBuffer) {
        inputMessageHandler.sendReadCompleteMessage(buffer)
    }


}

enum class ReadResult {
    REMOTE_CLOSE, SUSPEND_READ, CONTINUE_READ, READ_EXCEPTION
}

enum class WriteResult {
    REMOTE_CLOSE, SUSPEND_WRITE, CONTINUE_WRITE, WRITE_EXCEPTION
}