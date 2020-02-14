package com.fireflysource.net.tcp.aio

import com.fireflysource.common.func.Callback
import com.fireflysource.common.io.*
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.AbstractConnection
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import com.fireflysource.net.tcp.buffer.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.ClosedChannelException
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
abstract class AbstractAioTcpConnection(
    id: Int,
    maxIdleTime: Long,
    private val socketChannel: AsynchronousSocketChannel,
    dispatcher: CoroutineDispatcher,
    private val aioTcpCoroutineDispatcher: TcpCoroutineDispatcher = AioTcpCoroutineDispatcher(dispatcher)
) : AbstractConnection(id, System.currentTimeMillis(), maxIdleTime), TcpConnection,
    TcpCoroutineDispatcher by aioTcpCoroutineDispatcher {

    companion object {
        private val log = SystemLogger.create(AbstractAioTcpConnection::class.java)
        private val closeFailureResult = Result<Void>(false, null, CloseRequestException())
        private val closedChannelException = ClosedChannelException()
        private val timeUnit = TimeUnit.SECONDS
        val startReadingException = StartReadingException()
    }

    private val inputChannel: Channel<ByteBuffer> = Channel(UNLIMITED)


    private val isInputShutdown: AtomicBoolean = AtomicBoolean(false)
    private val isOutputShutdown: AtomicBoolean = AtomicBoolean(false)
    private val socketChannelClosed: AtomicBoolean = AtomicBoolean(false)
    private val closeRequest: AtomicBoolean = AtomicBoolean(false)
    private val isReading: AtomicBoolean = AtomicBoolean(false)

    private val adaptiveBufferSize: AdaptiveBufferSize = AdaptiveBufferSize()
    private val closeCallbacks: MutableList<Callback> = mutableListOf()
    private val exceptionConsumers: MutableList<Consumer<Throwable>> = mutableListOf()

    private var receivedMessageConsumer: Consumer<ByteBuffer> = Consumer { buf ->
        inputChannel.offer(buf)
    }

    private val outputMessageHandler = OutputMessageHandler()

    private inner class OutputMessageHandler {
        private val outputMessageChannel: Channel<OutputMessage> = Channel(UNLIMITED)

        init {
            writeJob()
        }

        fun sendOutputMessage(output: OutputMessage) {
            if (isWriteable()) {
                outputMessageChannel.offer(output)
            } else {
                failed(output, closedChannelException)
            }
        }

        private fun writeJob() = coroutineScope.launch {
            while (isWriteable()) {
                val writeable = handleOutputMessage(outputMessageChannel.receive())
                if (!writeable) {
                    break
                }
            }
        }

        private suspend fun handleOutputMessage(outputMessage: OutputMessage): Boolean {
            lastWrittenTime = System.currentTimeMillis()
            return when (outputMessage) {
                is OutputBuffer, is OutputBuffers, is OutputBufferList -> writeBuffers(outputMessage)
                is ShutdownOutput -> shutdownOutputAndClose(outputMessage)
            }
        }

        private fun shutdownOutputAndClose(outputMessage: ShutdownOutput): Boolean {
            shutdownOutputAndClose()
            outputMessage.result.accept(Result.SUCCESS)
            return false
        }

        private fun shutdownOutputAndClose() {
            shutdownOutput()
            if (isShutdownInput) {
                closeNow()
            }
        }

        private suspend fun write(output: OutputMessage): Long = when (output) {
            is OutputBuffer -> socketChannel.writeAwait(output.buffer, maxIdleTime, timeUnit).toLong()
            is OutputBuffers -> socketChannel.writeAwait(
                output.buffers, output.getCurrentOffset(), output.getCurrentLength(),
                maxIdleTime, timeUnit
            )
            is OutputBufferList -> socketChannel.writeAwait(
                output.buffers, output.getCurrentOffset(), output.getCurrentLength(),
                maxIdleTime, timeUnit
            )
            else -> throw IllegalArgumentException("The output message cannot write.")
        }

        private suspend fun writeBuffers(outputBuffers: OutputMessage): Boolean {
            var totalLength = 0L
            var success = true
            var exception: Exception? = null
            while (outputBuffers.hasRemaining()) {
                try {
                    val writtenLength = write(outputBuffers)
                    if (writtenLength < 0) {
                        success = false
                        exception = closedChannelException
                        break
                    } else {
                        writtenBytes += writtenLength
                        totalLength += writtenLength
                    }
                } catch (e: Exception) {
                    log.warn(e) { "The TCP connection writing exception. id: $id" }
                    success = false
                    exception = e
                    break
                }
            }

            fun complete() {
                when (outputBuffers) {
                    is OutputBuffer -> outputBuffers.result.accept(Result(true, totalLength.toInt(), null))
                    is OutputBuffers -> outputBuffers.result.accept(Result(true, totalLength, null))
                    is OutputBufferList -> outputBuffers.result.accept(Result(true, totalLength, null))
                }
            }

            if (success) {
                log.debug { "TCP connection writes buffers total length: $totalLength" }
                complete()
            } else {
                shutdownOutputAndClose()
                failed(outputBuffers, exception)
            }
            return success
        }

        private fun failed(outputBuffers: OutputMessage, exception: Exception?) {
            when (outputBuffers) {
                is OutputBuffer -> outputBuffers.result.accept(Result(false, -1, exception))
                is OutputBuffers -> outputBuffers.result.accept(Result(false, -1, exception))
                is OutputBufferList -> outputBuffers.result.accept(Result(false, -1, exception))
                is ShutdownOutput -> outputBuffers.result.accept(Result.createFailedResult(exception))
            }
        }
    }


    private inner class InputMessageHandler {

        private val inputMessageChannel: Channel<InputMessage> = Channel(UNLIMITED)

        init {
            readJob()
        }

        fun sendInputMessage(input: InputMessage) {
            if (isReadable()) {
                inputMessageChannel.offer(input)
            } else {
                failed(input, closedChannelException)
            }
        }

        private fun readJob() = coroutineScope.launch {
            while (isReadable()) {
                val input = inputMessageChannel.receive()
                val readable = handleInputMessage(input)
                if (!readable) {
                    break
                }
            }
        }

        private suspend fun handleInputMessage(input: InputMessage): Boolean {
            lastReadTime = System.currentTimeMillis()
            return when (input) {
                is InputBuffer, is InputBuffers -> readBuffers(input)
                is ShutdownInput -> shutdownInputAndClose()
            }
        }

        private suspend fun readBuffers(input: InputMessage): Boolean {
            var success = true
            var exception: Exception? = null
            var length = 0L
            try {

                length = read(input)
                if (length < 0) {
                    success = false
                    exception = closedChannelException
                }
            } catch (e: InterruptedByTimeoutException) {
                log.warn { "The TCP connection reading timeout. $id" }
                success = false
                exception = e
            } catch (e: Exception) {
                log.warn(e) { "The TCP connection reading exception. $id" }
                success = false
                exception = e
            }

            fun complete() {
                when (input) {
                    is InputBuffer -> input.result.accept(Result(true, length.toInt(), null))
                    is InputBuffers -> input.result.accept(Result(true, length, null))
                }
            }

            if (success) {
                log.debug { "TCP connection reads buffers total length: $length" }
                complete()
            } else {
                shutdownInputAndClose()
                failed(input, exception)
            }
            return success
        }

        private fun shutdownInputAndClose(): Boolean {
            shutdownInput()
            if (isShutdownOutput) {
                closeNow()
            }
            return false
        }

        private suspend fun read(input: InputMessage): Long = when (input) {
            is InputBuffer -> socketChannel.readAwait(input.buffer, maxIdleTime, timeUnit).toLong()
            is InputBuffers -> socketChannel.readAwait(input.buffers, input.offset, input.length, maxIdleTime, timeUnit)
            else -> throw IllegalArgumentException("The input message cannot read.")
        }

        private fun failed(input: InputMessage, exception: Exception?) {
            when (input) {
                is InputBuffer -> input.result.accept(Result(false, -1, exception))
                is InputBuffers -> input.result.accept(Result(false, -1, exception))
                is ShutdownInput -> input.result.accept(Result.createFailedResult(exception))
            }
        }
    }

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        outputMessageHandler.sendOutputMessage(OutputBuffer(byteBuffer, result))
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>, offset: Int, length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        outputMessageHandler.sendOutputMessage(OutputBuffers(byteBuffers, offset, length, result))
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>, offset: Int, length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        outputMessageHandler.sendOutputMessage(OutputBufferList(byteBufferList, offset, length, result))
        return this
    }

    private fun isWriteable() = !isOutputShutdown.get() && !socketChannelClosed.get()

    private fun isReadable() = !isInputShutdown.get() && !socketChannelClosed.get()

    override fun onRead(messageConsumer: Consumer<ByteBuffer>): TcpConnection {
        if (!isReading()) {
            receivedMessageConsumer = messageConsumer
        } else {
            throw startReadingException
        }
        return this
    }

    override fun startReading(): TcpConnection {
        if (isReading.compareAndSet(false, true)) {
            readJob()
        }
        return this
    }

    private fun readJob() = coroutineScope.launch {
        log.info { "The TCP connection starts reading automatically. id: $id" }

        while (isReading()) {
            try {
                val bufferSize = adaptiveBufferSize.getBufferSize()
                val buffer = BufferUtils.allocate(bufferSize)
                val position = buffer.flipToFill()
                log.debug { "Allocate TCP read buffer size: $bufferSize" }

                lastReadTime = System.currentTimeMillis()
                val readLength = socketChannel.readAwait(buffer, maxIdleTime, timeUnit)
                if (readLength < 0) {
                    suspendReading()
                    closeNow()
                    break
                } else {
                    adaptiveBufferSize.update(readLength)
                    readBytes += readLength
                    buffer.flipToFlush(position)
                    try {
                        receivedMessageConsumer.accept(buffer)
                    } catch (e: Exception) {
                        exceptionConsumers.forEach { it.accept(e) }
                    }
                }
            } catch (e: InterruptedByTimeoutException) {
                log.warn { "The TCP connection reading timeout. $id" }
                suspendReading()
                closeNow()
                break
            } catch (e: Exception) {
                log.warn(e) { "The TCP connection reading exception. $id" }
                suspendReading()
                closeNow()
                break
            }
        }

        log.info { "The TCP connection stops receiving messages. $id" }
    }

    override fun getInputChannel(): Channel<ByteBuffer> = inputChannel

    override fun isReading(): Boolean = isReading.get()

    override fun suspendReading(): TcpConnection {
        isReading.set(false)
        return this
    }


    override fun onClose(callback: Callback): TcpConnection {
        closeCallbacks.add(callback)
        return this
    }

    override fun close(result: Consumer<Result<Void>>): TcpConnection {
        if (closeRequest.compareAndSet(false, true)) {
            outputMessageHandler.sendOutputMessage(ShutdownOutput(result))
        } else {
            result.accept(closeFailureResult)
        }
        return this
    }

    override fun close() {
        close(discard())
    }

    override fun shutdownInput(): TcpConnection {
        if (isInputShutdown.compareAndSet(false, true)) {
            try {
                socketChannel.shutdownInput()
            } catch (e: ClosedChannelException) {
                log.warn { "The channel closed. $id" }
            } catch (e: IOException) {
                log.warn { "Shutdown input exception. $id" }
            }
        }
        return this
    }

    override fun shutdownOutput(): TcpConnection {
        if (isOutputShutdown.compareAndSet(false, true)) {
            try {
                socketChannel.shutdownOutput()
            } catch (e: ClosedChannelException) {
                log.warn { "The channel closed. $id" }
            } catch (e: IOException) {
                log.warn { "Shutdown output exception. $id" }
            }
        }
        return this
    }

    override fun isShutdownInput(): Boolean = isInputShutdown.get()

    override fun isShutdownOutput(): Boolean = isOutputShutdown.get()

    override fun closeNow(): TcpConnection {
        if (socketChannelClosed.compareAndSet(false, true)) {
            closeTime = System.currentTimeMillis()
            try {
                socketChannel.close()
            } catch (e: Exception) {
                log.warn(e) { "Close socket channel exception. $id" }
            }

            try {
                coroutineScope.cancel()
            } catch (e: Exception) {
                log.warn(e) { "Cancel writing job exception. $id" }
            }

            try {
                closeCallbacks.forEach { it.call() }
            } catch (e: Exception) {
                log.warn(e) { "The TCP connection close callback exception. $id" }
            }

            log.info { "The TCP connection close success. $id" }
        }
        return this
    }


    override fun onException(exception: Consumer<Throwable>): TcpConnection {
        exceptionConsumers.add(exception)
        return this
    }

    override fun isClosed(): Boolean = socketChannelClosed.get()

    override fun getLocalAddress(): InetSocketAddress = socketChannel.localAddress as InetSocketAddress

    override fun getRemoteAddress(): InetSocketAddress = socketChannel.remoteAddress as InetSocketAddress
}

class CloseRequestException : IllegalStateException("The close request has been sent")

class StartReadingException : IllegalStateException("The connection has started reading.")