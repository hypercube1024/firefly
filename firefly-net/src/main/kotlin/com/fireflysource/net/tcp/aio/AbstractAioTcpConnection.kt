package com.fireflysource.net.tcp.aio

import com.fireflysource.common.func.Callback
import com.fireflysource.common.io.*
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.createFailedResult
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.AbstractConnection
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
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
    private val outputChannel: Channel<OutputMessage> = Channel(UNLIMITED)

    private val inputShutdownState: AtomicBoolean = AtomicBoolean(false)
    private val outputShutdownState: AtomicBoolean = AtomicBoolean(false)
    private val socketChannelClosed: AtomicBoolean = AtomicBoolean(false)
    private val closeRequest: AtomicBoolean = AtomicBoolean(false)
    private val readingState: AtomicBoolean = AtomicBoolean(false)

    private val adaptiveBufferSize: AdaptiveBufferSize = AdaptiveBufferSize()
    private val closeCallbacks: MutableList<Callback> = mutableListOf()
    private val exceptionConsumers: MutableList<Consumer<Throwable>> = mutableListOf()

    private var receivedMessageConsumer: Consumer<ByteBuffer> = Consumer { buf ->
        inputChannel.offer(buf)
    }

    init {
        flushDataJob()
    }

    private fun flushDataJob() = coroutineScope.launch {
        while (isWriteable()) {
            val writeable = flushData(outputChannel.receive())
            if (!writeable) {
                break
            }
        }
    }

    private suspend fun flushData(outputMessage: OutputMessage): Boolean {
        lastWrittenTime = System.currentTimeMillis()
        return when (outputMessage) {
            is Buffer -> flushBuffer(outputMessage)
            is Buffers -> flushBuffers(outputMessage)
            is BufferList -> flushBuffers(outputMessage)
            is Shutdown -> shutdown(outputMessage)
        }
    }

    private fun shutdown(outputMessage: Shutdown): Boolean {
        shutdownOutputAndInput()
        outputMessage.result.accept(Result.SUCCESS)
        return false
    }

    private suspend fun flushBuffers(outputBuffers: Buffers): Boolean {
        var totalLength = 0L
        var success = true
        var exception: Exception? = null
        while (outputBuffers.hasRemaining()) {
            try {
                val offset = outputBuffers.getCurrentOffset()
                val length = outputBuffers.getCurrentLength()
                val writtenLength =
                    socketChannel.writeAwait(outputBuffers.getBuffers(), offset, length, maxIdleTime, timeUnit)
                log.debug { "TCP connection writes data. id: ${id}, offset: ${offset}, currentOffset: ${outputBuffers.getCurrentOffset()}, writtenLength: $writtenLength" }
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
        if (success) {
            outputBuffers.getResult().accept(Result(true, totalLength, null))
        } else {
            shutdownOutputAndInput()
            outputBuffers.getResult().accept(Result(false, -1, exception))
        }
        return success
    }

    private suspend fun flushBuffer(outputMessage: Buffer): Boolean {
        var totalLength = 0
        var success = true
        var exception: Exception? = null
        while (outputMessage.buffer.hasRemaining()) {
            try {
                val writtenLength = socketChannel.writeAwait(outputMessage.buffer, maxIdleTime, timeUnit)
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
        if (success) {
            outputMessage.result.accept(Result(true, totalLength, null))
        } else {
            shutdownOutputAndInput()
            outputMessage.result.accept(Result(false, -1, exception))
        }
        return true
    }

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        if (isWriteable()) {
            outputChannel.offer(Buffer(byteBuffer, result))
        } else {
            result.accept(createFailedResult(-1, closedChannelException))
        }
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>, offset: Int, length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        if (isWriteable()) {
            outputChannel.offer(Buffers(byteBuffers, offset, length, result))
        } else {
            result.accept(createFailedResult(-1, closedChannelException))
        }
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>, offset: Int, length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        if (isWriteable()) {
            outputChannel.offer(BufferList(byteBufferList, offset, length, result))
        } else {
            result.accept(createFailedResult(-1, closedChannelException))
        }
        return this
    }

    private fun isWriteable() = !outputShutdownState.get() && !socketChannelClosed.get()


    override fun onRead(messageConsumer: Consumer<ByteBuffer>): TcpConnection {
        if (!isReading) {
            receivedMessageConsumer = messageConsumer
        } else {
            throw startReadingException
        }
        return this
    }

    override fun startReading(): TcpConnection {
        if (readingState.compareAndSet(false, true)) {
            readJob()
        }
        return this
    }

    private fun readJob() = coroutineScope.launch {
        log.info { "The TCP connection starts reading automatically. id: $id" }

        while (isReading) {
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

    override fun isReading(): Boolean = readingState.get()

    override fun suspendReading(): TcpConnection {
        readingState.set(false)
        return this
    }


    override fun onClose(callback: Callback): TcpConnection {
        closeCallbacks.add(callback)
        return this
    }

    override fun close(result: Consumer<Result<Void>>): TcpConnection {
        if (closeRequest.compareAndSet(false, true)) {
            outputChannel.offer(Shutdown(result))
        } else {
            result.accept(closeFailureResult)
        }
        return this
    }

    override fun close() {
        close(discard())
    }

    private fun shutdownOutputAndInput() {
        shutdownOutput()
        shutdownInput()
    }

    override fun shutdownInput(): TcpConnection {
        if (inputShutdownState.compareAndSet(false, true)) {
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
        if (outputShutdownState.compareAndSet(false, true)) {
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

    override fun isShutdownInput(): Boolean = inputShutdownState.get()

    override fun isShutdownOutput(): Boolean = outputShutdownState.get()

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


sealed class OutputMessage

class Buffer(val buffer: ByteBuffer, val result: Consumer<Result<Int>>) : OutputMessage()

open class Buffers(
    private val buffers: Array<ByteBuffer>,
    offset: Int,
    length: Int,
    private val result: Consumer<Result<Long>>
) : OutputMessage() {

    init {
        require(offset >= 0) { "The offset must be greater than or equal the 0" }
        require(length > 0) { "The length must be greater than 0" }
        require(offset < buffers.size) { "The offset must be less than the buffer size" }
        require((offset + length) <= buffers.size) { "The length must be less than or equal the buffer size" }
    }

    private val maxSize = offset + length
    private val lastIndex = maxSize - 1
    private var currentOffset = offset

    fun getBuffers(): Array<ByteBuffer> = buffers

    fun getResult(): Consumer<Result<Long>> = result

    fun getCurrentOffset(): Int {
        val buffers = getBuffers()
        for (i in currentOffset..lastIndex) {
            if (buffers[i].hasRemaining()) {
                currentOffset = i
                return i
            }
        }
        return maxSize
    }

    fun getCurrentLength(): Int {
        return maxSize - getCurrentOffset()
    }

    fun hasRemaining(): Boolean {
        return getCurrentOffset() < maxSize
    }

    fun getLastIndex(): Int = lastIndex
}

class BufferList(
    bufferList: List<ByteBuffer>,
    offset: Int,
    length: Int,
    result: Consumer<Result<Long>>
) : Buffers(bufferList.toTypedArray(), offset, length, result)

class Shutdown(val result: Consumer<Result<Void>>) : OutputMessage()