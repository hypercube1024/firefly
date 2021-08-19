package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.consumeAll
import com.fireflysource.common.exception.UnknownTypeException
import com.fireflysource.common.func.Callback
import com.fireflysource.common.io.*
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.createFailedResult
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
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.ClosedChannelException
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
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
    inputBufferSize: Int,
    private val aioTcpCoroutineDispatcher: TcpCoroutineDispatcher = AioTcpCoroutineDispatcher(id, dispatcher)
) : AbstractConnection(id, System.currentTimeMillis(), maxIdleTime), TcpConnection,
    TcpCoroutineDispatcher by aioTcpCoroutineDispatcher {

    companion object {
        private val log = SystemLogger.create(AbstractAioTcpConnection::class.java)
        private val timeUnit = TimeUnit.SECONDS
    }

    private val isInputShutdown: AtomicBoolean = AtomicBoolean(false)
    private val isOutputShutdown: AtomicBoolean = AtomicBoolean(false)
    private val socketChannelClosed: AtomicBoolean = AtomicBoolean(false)
    private val closeRequest: AtomicBoolean = AtomicBoolean(false)
    private val closeCallbacks: MutableList<Callback> = mutableListOf()
    private val outputMessageHandler = OutputMessageHandler()
    private val inputMessageHandler = InputMessageHandler(inputBufferSize)
    private val closeResultChannel: Channel<Consumer<Result<Void>>> = Channel(UNLIMITED)

    private inner class OutputMessageHandler {
        private val outputMessageChannel: Channel<OutputMessage> = Channel(UNLIMITED)
        private var writeTimeout = maxIdleTime

        init {
            writeJob()
        }

        fun sendOutputMessage(output: OutputMessage) =
            outputMessageChannel.trySend(output)

        private fun writeJob() = coroutineScope.launch {
            while (true) {
                handleOutputMessage(outputMessageChannel.receive())
            }
        }.invokeOnCompletion { cause ->
            val e = cause ?: ClosedChannelException()
            outputMessageChannel.consumeAll { message ->
                when (message) {
                    is OutputBuffer -> message.result.accept(createFailedResult(-1, e))
                    is OutputBuffers -> message.result.accept(createFailedResult(-1, e))
                    is OutputBufferList -> message.result.accept(createFailedResult(-1, e))
                    is ShutdownOutput -> message.result.accept(createFailedResult(e))
                    else -> throw UnknownTypeException("Unknown output message. $message")
                }
            }
            closeResultChannel.consumeAll { it.accept(Result.SUCCESS) }
        }

        private suspend fun handleOutputMessage(output: OutputMessage) {
            when (output) {
                is OutputDataMessage -> writeBuffers(output)
                is ShutdownOutput -> shutdownOutputAndClose(output)
                is SetWriteTimeout -> if (output.timeout > 0) {
                    writeTimeout = output.timeout
                    log.info("Set write timeout: $writeTimeout, id: $id")
                }
                else -> throw UnknownTypeException("Unknown output message. $output")
            }
        }

        private suspend fun write(output: OutputDataMessage): Long = when (output) {
            is OutputBuffer -> socketChannel.writeAwait(output.buffer, writeTimeout, timeUnit).toLong()
            is OutputBuffers -> socketChannel.writeAwait(
                output.buffers, output.getCurrentOffset(), output.getCurrentLength(),
                writeTimeout, timeUnit
            )
            is OutputBufferList -> socketChannel.writeAwait(
                output.buffers, output.getCurrentOffset(), output.getCurrentLength(),
                writeTimeout, timeUnit
            )
            else -> throw UnknownTypeException("The output message cannot write.")
        }

        private suspend fun writeBuffers(output: OutputDataMessage): Boolean {
            lastWrittenTime = System.currentTimeMillis()
            var totalLength = 0L
            var success = true
            var exception: Exception? = null
            while (output.hasRemaining()) {
                try {
                    val writtenLength = write(output)
                    if (writtenLength < 0) {
                        success = false
                        exception = ClosedChannelException()
                        break
                    } else {
                        writtenBytes += writtenLength
                        totalLength += writtenLength
                    }
                } catch (e: InterruptedByTimeoutException) {
                    log.warn { "The TCP connection writing timeout. id: $id" }
                    success = false
                    exception = e
                    break
                } catch (e: Exception) {
                    log.warn { "The TCP connection writing exception. ${e.message} id: $id" }
                    success = false
                    exception = e
                    break
                }
            }

            fun complete() {
                when (output) {
                    is OutputBuffer -> output.result.accept(Result(true, totalLength.toInt(), null))
                    is OutputBuffers -> output.result.accept(Result(true, totalLength, null))
                    is OutputBufferList -> output.result.accept(Result(true, totalLength, null))
                    else -> throw UnknownTypeException("The output message type error")
                }
            }

            if (success) {
                log.debug { "TCP connection writes buffers total length: $totalLength" }
                complete()
            } else {
                shutdownOutputAndClose()
                failed(output, exception)
            }
            return success
        }

        private fun failed(outputBuffers: OutputDataMessage, exception: Exception?) {
            when (outputBuffers) {
                is OutputBuffer -> outputBuffers.result.accept(Result(false, -1, exception))
                is OutputBuffers -> outputBuffers.result.accept(Result(false, -1, exception))
                is OutputBufferList -> outputBuffers.result.accept(Result(false, -1, exception))
                else -> throw UnknownTypeException("The output message type error")
            }
        }

        private fun shutdownOutputAndClose(output: ShutdownOutput) {
            shutdownOutputAndClose()
            output.result.accept(Result.SUCCESS)
        }

        private fun shutdown() {
            if (isOutputShutdown.compareAndSet(false, true)) {
                try {
                    socketChannel.shutdownOutput()
                } catch (e: ClosedChannelException) {
                    log.warn { "The channel closed. $id" }
                } catch (e: IOException) {
                    log.warn { "Shutdown output exception. $id" }
                }
            }
        }

        private fun shutdownOutputAndClose() {
            if (isClosed) return

            this@OutputMessageHandler.shutdown()
            log.debug { "TCP connection shutdown output. id $id, out: $isOutputShutdown, in: $isInputShutdown, socket: ${!socketChannel.isOpen}" }
            if (isShutdownInput) {
                closeNow()
            }
        }
    }

    private inner class InputMessageHandler(inputBufferSize: Int) {

        private val inputMessageChannel: Channel<InputMessage> = Channel(UNLIMITED)
        private val inputBuffer = BufferUtils.allocateDirect(inputBufferSize)
        private var readTimeout = maxIdleTime

        init {
            readJob()
        }

        fun sendInputMessage(input: InputMessage): ChannelResult<Unit> {
            if (input is ShutdownInput) {
                this@InputMessageHandler.shutdown()
            }
            return inputMessageChannel.trySend(input)
        }

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
            when (input) {
                is InputBuffer -> readBuffers(input)
                is ShutdownInput -> shutdownInputAndClose()
                is SetReadTimeout -> if (input.timeout > 0) {
                    readTimeout = input.timeout
                    log.info("Set read timeout: $readTimeout, id: $id")
                }
            }
        }

        private suspend fun readBuffers(input: InputBuffer): Boolean {
            lastReadTime = System.currentTimeMillis()
            var success = true
            var exception: Exception? = null
            var length = 0
            try {
                val pos = inputBuffer.flipToFill()
                length = socketChannel.readAwait(inputBuffer, readTimeout, timeUnit)
                inputBuffer.flipToFlush(pos)
                if (length < 0) {
                    success = false
                    exception = ClosedChannelException()
                } else {
                    readBytes += length
                }
            } catch (e: InterruptedByTimeoutException) {
                log.warn { "The TCP connection reading timeout. id: $id" }
                success = false
                exception = e
            } catch (e: Exception) {
                log.warn { "The TCP connection reading exception. ${e.message} id: $id" }
                success = false
                exception = e
            }

            if (success) {
                log.debug { "TCP connection reads buffers total length: $length" }
                inputBuffer.copy().also { input.bufferFuture.complete(it) }
            } else {
                shutdownInputAndClose()
                failed(input, exception)
            }
            BufferUtils.clear(inputBuffer)
            return success
        }

        private fun failed(input: InputMessage, e: Exception?) {
            if (input is InputBuffer) {
                input.bufferFuture.completeExceptionally(e)
            }
        }

        private fun shutdown() {
            if (isInputShutdown.compareAndSet(false, true)) {
                try {
                    socketChannel.shutdownInput()
                } catch (e: ClosedChannelException) {
                    log.warn { "The channel closed. $id" }
                } catch (e: IOException) {
                    log.warn { "Shutdown input exception. $id" }
                }
            }
        }

        private fun shutdownInputAndClose() {
            if (isClosed) {
                return
            }

            this@InputMessageHandler.shutdown()
            log.debug { "TCP connection shutdown input. id $id, out: $isOutputShutdown, in: $isInputShutdown, socket: ${!socketChannel.isOpen}" }
            if (isShutdownOutput) {
                closeNow()
            } else {
                this@AbstractAioTcpConnection.shutdownOutput()
            }
        }
    }

    override fun read(): CompletableFuture<ByteBuffer> {
        val future = CompletableFuture<ByteBuffer>()
        if (inputMessageHandler.sendInputMessage(InputBuffer(future)).isFailure) {
            future.completeExceptionally(ClosedChannelException())
        }
        return future
    }

    override fun setReadTimeout(timeout: Long) {
        inputMessageHandler.sendInputMessage(SetReadTimeout(timeout))
    }

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        if (outputMessageHandler.sendOutputMessage(OutputBuffer(byteBuffer, result)).isFailure) {
            result.accept(createFailedResult(-1, ClosedChannelException()))
        }
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>, offset: Int, length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        if (outputMessageHandler.sendOutputMessage(OutputBuffers(byteBuffers, offset, length, result)).isFailure) {
            result.accept(createFailedResult(-1L, ClosedChannelException()))
        }
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>, offset: Int, length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        if (outputMessageHandler.sendOutputMessage(
                OutputBufferList(
                    byteBufferList,
                    offset,
                    length,
                    result
                )
            ).isFailure
        ) {
            result.accept(createFailedResult(-1L, ClosedChannelException()))
        }
        return this
    }

    override fun setWriteTimeout(timeout: Long) {
        outputMessageHandler.sendOutputMessage(SetWriteTimeout(timeout))
    }

    override fun flush(result: Consumer<Result<Void>>): TcpConnection {
        result.accept(Result.SUCCESS)
        return this
    }

    override fun getBufferSize(): Int = 0


    override fun onClose(callback: Callback): TcpConnection {
        closeCallbacks.add(callback)
        return this
    }

    override fun close(result: Consumer<Result<Void>>): TcpConnection {
        if (closeRequest.compareAndSet(false, true)) {
            if (isClosed) {
                result.accept(Result.SUCCESS)
            } else {
                closeResultChannel.trySend(result)
                outputMessageHandler.sendOutputMessage(ShutdownOutput {
                    if (inputMessageHandler.sendInputMessage(ShutdownInput).isFailure) {
                        result.accept(createFailedResult(ClosedChannelException()))
                    }
                })
            }
        } else {
            result.accept(Result.SUCCESS)
        }
        return this
    }

    override fun close() {
        close(discard())
    }

    override fun shutdownInput(): TcpConnection {
        inputMessageHandler.sendInputMessage(ShutdownInput)
        return this
    }

    override fun shutdownOutput(): TcpConnection {
        outputMessageHandler.sendOutputMessage(ShutdownOutput(discard()))
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
                log.warn { "Close socket channel exception. ${e.message} id: $id" }
            }

            closeCallbacks.forEach {
                try {
                    it.call()
                } catch (e: Exception) {
                    log.warn { "The TCP connection close callback exception. ${e.message} id: $id" }
                }
            }

            try {
                coroutineScope.cancel(CancellationException("Cancel TCP coroutine exception. id: $id"))
            } catch (e: Throwable) {
                log.warn { "Cancel TCP coroutine exception. ${e.message} id: $id" }
            }

            closeResultChannel.consumeAll { it.accept(Result.SUCCESS) }

            log.info { "The TCP connection close success. id: $id, out: $isOutputShutdown, in: $isInputShutdown, socket: ${!socketChannel.isOpen}" }
        }
        return this
    }


    override fun isClosed(): Boolean = socketChannelClosed.get()

    override fun isInvalid(): Boolean =
        closeRequest.get() || isShutdownInput || isShutdownOutput || socketChannelClosed.get()

    override fun getLocalAddress(): InetSocketAddress = socketChannel.localAddress as InetSocketAddress

    override fun getRemoteAddress(): InetSocketAddress = socketChannel.remoteAddress as InetSocketAddress
}