package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.func.Callback
import com.fireflysource.common.io.aRead
import com.fireflysource.common.io.aWrite
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.EMPTY_CONSUMER_RESULT
import com.fireflysource.common.sys.SystemLogger
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
    private val connectionId: Int,
    private val socketChannel: AsynchronousSocketChannel,
    private val timeout: Long,
    private val messageThread: CoroutineDispatcher
) : TcpConnection {

    companion object {
        private val log = SystemLogger.create(AbstractTcpConnection::class.java)
        private val closeFailureResult = Result<Void>(false, null, CloseRequestException())
        private val channelClosedException = ChannelClosedException()
        private val timeUnit = TimeUnit.SECONDS
        val startReadingException = StartReadingException()
    }

    private val openTimestamp: Long = System.currentTimeMillis()
    private val inChannel: Channel<ByteBuffer> = Channel(UNLIMITED)
    private val outChannel: Channel<Message> = Channel(UNLIMITED)

    private val inputShutdownState: AtomicBoolean = AtomicBoolean(false)
    private val outputShutdownState: AtomicBoolean = AtomicBoolean(false)
    private val socketChannelClosed: AtomicBoolean = AtomicBoolean(false)
    private val closeRequest: AtomicBoolean = AtomicBoolean(false)
    private var closeConsumer: Consumer<Result<Void>> = EMPTY_CONSUMER_RESULT

    private val autoReadState: AtomicBoolean = AtomicBoolean(false)

    private val adaptiveBufferSize: AdaptiveBufferSize = AdaptiveBufferSize()
    private val closeCallbacks: MutableList<Callback> = mutableListOf()
    private val exceptionConsumers: MutableList<Consumer<Throwable>> = mutableListOf()

    @Volatile
    private var attachmentObj: Any? = null
    private var closeTimestamp: Long = 0
    private var lastReadTimestamp: Long = 0
    private var lastWrittenTimestamp: Long = 0
    private var readByteCount: Long = 0
    private var writtenByteCount: Long = 0

    private var receivedMessageConsumer: Consumer<ByteBuffer> = Consumer { buf ->
        inChannel.offer(buf)
    }

    private val writingJob = launchWritingJob()

    override fun getCoroutineDispatcher(): CoroutineDispatcher = messageThread

    override fun getAttachment(): Any? = attachmentObj

    override fun setAttachment(obj: Any?) {
        attachmentObj = obj
    }

    override fun getId(): Int = connectionId

    override fun getOpenTime(): Long = openTimestamp

    override fun getCloseTime(): Long = closeTimestamp

    override fun getDuration(): Long = if (isClosed) {
        closeTime - openTime
    } else {
        System.currentTimeMillis() - openTimestamp
    }

    override fun getLastReadTime(): Long = lastReadTimestamp

    override fun getLastWrittenTime(): Long = lastWrittenTimestamp

    override fun getLastActiveTime(): Long = Math.max(lastReadTime, lastWrittenTime)

    override fun getReadBytes(): Long = readByteCount

    override fun getWrittenBytes(): Long = writtenByteCount

    override fun getIdleTime(): Long = System.currentTimeMillis() - lastActiveTime

    override fun getMaxIdleTime(): Long = timeout

    override fun isClosed(): Boolean = socketChannelClosed.get()

    override fun getLocalAddress(): InetSocketAddress = socketChannel.localAddress as InetSocketAddress

    override fun getRemoteAddress(): InetSocketAddress = socketChannel.remoteAddress as InetSocketAddress

    override fun onRead(messageConsumer: Consumer<ByteBuffer>): TcpConnection {
        if (!isStartReading) {
            receivedMessageConsumer = messageConsumer
        } else {
            throw startReadingException
        }
        return this
    }

    private fun launchWritingJob() = launchGlobally(messageThread) {
        while (!isShutdownOutput) {
            writeMessage(outChannel.receive())
        }
    }

    private suspend fun writeMessage(message: Message) {
        lastWrittenTimestamp = System.currentTimeMillis()
        when (message) {
            is Buffer -> {
                while (message.buffer.hasRemaining()) {
                    try {
                        val count = socketChannel.aWrite(message.buffer, timeout, timeUnit)
                        message.result.accept(Result(true, count, null))
                        if (count < 0) {
                            shutdownInputAndOutput()
                            break
                        } else {
                            writtenByteCount += count
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
            }
            is Buffers -> {
                while (message.hasRemaining()) {
                    try {
                        val offset = message.getCurrentOffset()
                        val length = message.getCurrentLength()

                        val count = socketChannel.aWrite(message.buffers, offset, length, timeout, timeUnit)
                        message.result.accept(Result(true, count, null))
                        if (count < 0) {
                            shutdownInputAndOutput()
                            break
                        } else {
                            writtenByteCount += count
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
            }
            is BufferList -> {
                while (message.hasRemaining()) {
                    try {
                        val offset = message.getCurrentOffset()
                        val length = message.getCurrentLength()

                        val count = socketChannel.aWrite(
                            message.bufferList.toTypedArray(),
                            offset,
                            length,
                            timeout,
                            timeUnit
                        )
                        message.result.accept(Result(true, count, null))
                        if (count < 0) {
                            shutdownInputAndOutput()
                            break
                        } else {
                            writtenByteCount += count
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
            }
            is Shutdown -> {
                shutdownInputAndOutput()
            }
        }
    }

    override fun startReading(): TcpConnection {
        if (autoReadState.compareAndSet(false, true)) {
            launchGlobally(messageThread) {
                log.debug { "start automatic reading" }

                while (true) {
                    val buf = ByteBuffer.allocate(adaptiveBufferSize.getBufferSize())
                    try {
                        lastReadTimestamp = System.currentTimeMillis()
                        val count = socketChannel.aRead(buf, timeout, timeUnit)
                        if (count < 0) {
                            shutdownAndClose()
                            break
                        } else {
                            adaptiveBufferSize.update(count)
                            readByteCount += count
                            buf.flip()
                            try {
                                receivedMessageConsumer.accept(buf)
                            } catch (e: Exception) {
                                exceptionConsumers.forEach { it.accept(e) }
                            }
                        }
                    } catch (e: InterruptedByTimeoutException) {
                        log.warn { "Tcp connection reading timeout. $id" }
                        shutdownAndClose()
                        break
                    } catch (e: Exception) {
                        log.warn(e) { "Tcp connection reading exception. $id" }
                        shutdownAndClose()
                        break
                    }
                }
            }
        }
        return this
    }

    override fun isStartReading(): Boolean = autoReadState.get()

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
        close(EMPTY_CONSUMER_RESULT)
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
                closeTimestamp = System.currentTimeMillis()
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