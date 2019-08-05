package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.func.Callback
import com.fireflysource.common.io.readAwait
import com.fireflysource.common.io.writeAwait
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.AbstractConnection
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
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
abstract class AbstractTcpConnection(
    id: Int,
    maxIdleTime: Long,
    private val socketChannel: AsynchronousSocketChannel,
    private val messageThread: CoroutineDispatcher
) : AbstractConnection(id, System.currentTimeMillis(), maxIdleTime), TcpConnection {

    companion object {
        private val log = SystemLogger.create(AbstractTcpConnection::class.java)
        private val closeFailureResult = Result<Void>(false, null, CloseRequestException())
        private val channelClosedException = ChannelClosedException()
        private val timeUnit = TimeUnit.SECONDS
        val startReadingException = StartReadingException()
    }

    private val inChannel: Channel<ByteBuffer> = Channel(UNLIMITED)
    private val outChannel: Channel<Message> = Channel(UNLIMITED)

    private val inputShutdownState: AtomicBoolean = AtomicBoolean(false)
    private val outputShutdownState: AtomicBoolean = AtomicBoolean(false)
    private val socketChannelClosed: AtomicBoolean = AtomicBoolean(false)
    private val closeRequest: AtomicBoolean = AtomicBoolean(false)
    private var closeConsumer: Consumer<Result<Void>> = discard()

    private val readingState: AtomicBoolean = AtomicBoolean(false)

    private val adaptiveBufferSize: AdaptiveBufferSize = AdaptiveBufferSize()
    private val closeCallbacks: MutableList<Callback> = mutableListOf()
    private val exceptionConsumers: MutableList<Consumer<Throwable>> = mutableListOf()

    private var receivedMessageConsumer: Consumer<ByteBuffer> = Consumer { buf ->
        inChannel.offer(buf)
    }

    private val writingJob = launchGlobally(messageThread) {
        while (!isShutdownOutput) {
            writeMessage(outChannel.receive())
        }
    }

    override fun getCoroutineDispatcher(): CoroutineDispatcher = messageThread

    override fun isClosed(): Boolean = socketChannelClosed.get()

    override fun getLocalAddress(): InetSocketAddress = socketChannel.localAddress as InetSocketAddress

    override fun getRemoteAddress(): InetSocketAddress = socketChannel.remoteAddress as InetSocketAddress

    private suspend fun writeMessage(message: Message) {
        lastWrittenTime = System.currentTimeMillis()
        when (message) {
            is Buffer -> {
                var len = 0
                while (message.buffer.hasRemaining()) {
                    try {
                        val count = socketChannel.writeAwait(message.buffer, maxIdleTime, timeUnit)
                        if (count < 0) {
                            shutdownInputAndOutput()
                            break
                        } else {
                            writtenBytes += count
                            len += count
                        }
                    } catch (e: InterruptedByTimeoutException) {
                        log.warn { "Tcp connection writing timeout. $id" }
                        shutdownInputAndOutput()
                        message.result.accept(Result(false, -1, e))
                        break
                    } catch (e: Exception) {
                        log.warn(e) { "Tcp connection writing exception. $id" }
                        shutdownInputAndOutput()
                        message.result.accept(Result(false, -1, e))
                        break
                    }
                }
                message.result.accept(Result(true, len, null))
            }
            is Buffers -> {
                var len = 0L
                while (message.hasRemaining()) {
                    try {
                        val offset = message.getCurrentOffset()
                        val length = message.getCurrentLength()

                        val count = socketChannel.writeAwait(message.buffers, offset, length, maxIdleTime, timeUnit)
                        if (count < 0) {
                            shutdownInputAndOutput()
                            break
                        } else {
                            writtenBytes += count
                            len += count
                        }
                    } catch (e: InterruptedByTimeoutException) {
                        log.warn { "Tcp connection writing timeout. $id" }
                        shutdownInputAndOutput()
                        message.result.accept(Result(false, -1, e))
                        break
                    } catch (e: Exception) {
                        log.warn(e) { "Tcp connection writing exception. $id" }
                        shutdownInputAndOutput()
                        message.result.accept(Result(false, -1, e))
                        break
                    }
                }
                message.result.accept(Result(true, len, null))
            }
            is BufferList -> {
                var len = 0L
                while (message.hasRemaining()) {
                    try {
                        val offset = message.getCurrentOffset()
                        val length = message.getCurrentLength()

                        val count = socketChannel.writeAwait(
                            message.bufferList.toTypedArray(), offset, length,
                            maxIdleTime, timeUnit
                        )
                        if (count < 0) {
                            shutdownInputAndOutput()
                            break
                        } else {
                            writtenBytes += count
                            len += count
                        }
                    } catch (e: InterruptedByTimeoutException) {
                        log.warn { "Tcp connection writing timeout. $id" }
                        shutdownInputAndOutput()
                        message.result.accept(Result(false, -1, e))
                        break
                    } catch (e: Exception) {
                        log.warn(e) { "Tcp connection writing exception. $id" }
                        shutdownInputAndOutput()
                        message.result.accept(Result(false, -1, e))
                        break
                    }
                }
                message.result.accept(Result(true, len, null))
            }
            is Shutdown -> {
                shutdownInputAndOutput()
            }
        }
    }

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
            launchGlobally(messageThread) {
                log.info { "The TCP connection $id starts automatic reading" }

                while (isReading) {
                    val buf = ByteBuffer.allocate(adaptiveBufferSize.getBufferSize())
                    try {
                        lastReadTime = System.currentTimeMillis()
                        val count = socketChannel.readAwait(buf, maxIdleTime, timeUnit)
                        if (count < 0) {
                            shutdownAndClose()
                            break
                        } else {
                            adaptiveBufferSize.update(count)
                            readBytes += count
                            buf.flip()
                            try {
                                receivedMessageConsumer.accept(buf)
                            } catch (e: Exception) {
                                exceptionConsumers.forEach { it.accept(e) }
                            }
                        }
                    } catch (e: InterruptedByTimeoutException) {
                        log.warn { "Tcp connection reading timeout. $id" }
                        suspendReading()
                        shutdownAndClose()
                        break
                    } catch (e: Exception) {
                        log.warn(e) { "Tcp connection reading exception. $id" }
                        suspendReading()
                        shutdownAndClose()
                        break
                    }
                }

                log.info { "The TCP connection $id stops receiving messages." }
            }
        }
        return this
    }

    override fun isReading(): Boolean = readingState.get()

    override fun suspendReading(): TcpConnection {
        readingState.set(false)
        return this
    }

    override fun onClose(callback: Callback): TcpConnection {
        closeCallbacks.add(callback)
        return this
    }

    override fun onException(exception: Consumer<Throwable>): TcpConnection {
        exceptionConsumers.add(exception)
        return this
    }

    override fun close(result: Consumer<Result<Void>>): TcpConnection {
        if (closeRequest.compareAndSet(false, true)) {
            closeConsumer = result
            outChannel.offer(Shutdown(result))
        } else {
            result.accept(closeFailureResult)
        }
        return this
    }

    override fun close() {
        close(discard())
    }

    private fun shutdownInput() {
        if (inputShutdownState.compareAndSet(false, true)) {
            try {
                socketChannel.shutdownInput()
            } catch (e: ClosedChannelException) {
                log.warn { "The channel is closed. $id" }
            } catch (e: IOException) {
                log.warn { "Shutdown input exception. $id" }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun shutdownOutput() {
        if (outputShutdownState.compareAndSet(false, true)) {
            try {
                writeRemainingMessage()
                socketChannel.shutdownOutput()
            } catch (e: ClosedChannelException) {
                log.warn { "The channel is closed. $id" }
            } catch (e: IOException) {
                log.warn { "Shutdown output exception. $id" }
            }
            writingJob.cancel()
        }
    }

    private suspend fun writeRemainingMessage() {
        while (true) {
            val msg = outChannel.poll() ?: break
            log.debug { "The channel will close. Writes the remaining data in the out channel." }
            writeMessage(msg)
        }
    }

    private suspend fun shutdownAndClose() {
        shutdownInputAndOutput()
        closeNow()
    }

    private suspend fun shutdownInputAndOutput() {
        shutdownOutput()
        shutdownInput()
    }

    override fun isShutdownInput(): Boolean = inputShutdownState.get()

    override fun isShutdownOutput(): Boolean = outputShutdownState.get()

    override fun closeNow(): TcpConnection {
        if (socketChannelClosed.compareAndSet(false, true)) {
            try {
                closeTime = System.currentTimeMillis()
                socketChannel.close()
                log.info { "tcp connection close success. $id" }
                closeCallbacks.forEach { it.call() }
            } catch (e: Exception) {
                log.warn(e) { "close socket channel exception. $id" }
            } finally {
                closeConsumer.accept(Result.SUCCESS)
            }
        }
        return this
    }

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        if (checkWriteState(result)) {
            outChannel.offer(Buffer(byteBuffer, result))
        }
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        if (checkWriteState(result)) {
            outChannel.offer(Buffers(byteBuffers, offset, length, result))
        }
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        if (checkWriteState(result)) {
            outChannel.offer(BufferList(byteBufferList, offset, length, result))
        }
        return this
    }

    private fun <T> checkWriteState(result: Consumer<Result<T>>): Boolean {
        return if (outputShutdownState.get() || socketChannelClosed.get()) {
            result.accept(Result<T>(false, null, channelClosedException))
            false
        } else {
            true
        }
    }

    override fun getInputChannel(): Channel<ByteBuffer> = inChannel
}

class CloseRequestException : IllegalStateException("The close request has been sent")

class ChannelClosedException : IllegalStateException("The socket channel is closed")

class StartReadingException : IllegalStateException("The connection has started reading.")

sealed class Message
class Buffer(val buffer: ByteBuffer, val result: Consumer<Result<Int>>) : Message()
class Buffers(
    val buffers: Array<ByteBuffer>,
    val offset: Int,
    val length: Int,
    val result: Consumer<Result<Long>>
) : Message() {

    init {
        require(offset >= 0) { "The offset must be greater than or equal the 0" }
        require(length > 0) { "The length must be greater than 0" }
        require(offset < buffers.size) { "The offset must be less than the buffer array size" }
        require((offset + length) <= buffers.size) { "The length must be less than or equal the buffer array size" }
    }

    fun getCurrentOffset(): Int {
        for (i in offset..buffers.lastIndex) {
            if (buffers[i].hasRemaining()) {
                return i
            }
        }
        return buffers.size
    }

    fun getCurrentLength(): Int = buffers.size - getCurrentOffset()

    fun hasRemaining(): Boolean = getCurrentOffset() < buffers.size
}

class BufferList(
    val bufferList: List<ByteBuffer>,
    val offset: Int,
    val length: Int,
    val result: Consumer<Result<Long>>
) : Message() {

    init {
        require(offset >= 0) { "The offset must be greater than or equal the 0" }
        require(length > 0) { "The length must be greater than 0" }
        require(offset < bufferList.size) { "The offset must be less than the buffer list size" }
        require((offset + length) <= bufferList.size) { "The length must be less than or equal the buffer list size" }
    }

    fun getCurrentOffset(): Int {
        for (i in offset..bufferList.lastIndex) {
            if (bufferList[i].hasRemaining()) {
                return i
            }
        }
        return bufferList.size
    }

    fun getCurrentLength(): Int = bufferList.size - getCurrentOffset()

    fun hasRemaining(): Boolean = getCurrentOffset() < bufferList.size
}

class Shutdown(val result: Consumer<Result<Void>>) : Message()