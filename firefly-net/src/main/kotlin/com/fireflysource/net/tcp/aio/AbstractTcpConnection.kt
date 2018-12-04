package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.launchWithAttr
import com.fireflysource.common.func.Callback
import com.fireflysource.common.io.aRead
import com.fireflysource.common.io.aWrite
import com.fireflysource.common.sys.CommonLogger
import com.fireflysource.net.tcp.Result
import com.fireflysource.net.tcp.Result.EMPTY_CONSUMER_RESULT
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.ClosedChannelException
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
abstract class AbstractTcpConnection(
    private val connectionId: Int,
    protected val socketChannel: AsynchronousSocketChannel,
    private val timeout: Long
                                    ) : TcpConnection {

    companion object {
        private val log = CommonLogger.create(AbstractTcpConnection::class.java)
        val dataTransThread: CoroutineDispatcher by lazy {
            Executors.newSingleThreadExecutor { Thread(it, "firefly-tcp-data-transfer-thread") }.asCoroutineDispatcher()
        }
        private val closeFailureResult = Result<Void>(false, null, CloseRequestException())
        private val channelClosedException = ChannelClosedException()
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

    override fun getAttachment(): Any? = attachmentObj

    override fun setAttachment(obj: Any?) {
        attachmentObj = obj
    }

    override fun getId(): Int = connectionId

    override fun getOpenTime(): Long = openTimestamp

    override fun getCloseTime(): Long = closeTimestamp

    override fun getDuration(): Long = System.currentTimeMillis() - openTimestamp

    override fun getLastReadTime(): Long = lastReadTimestamp

    override fun getLastWrittenTime(): Long = lastWrittenTimestamp

    override fun getLastActiveTime(): Long =
        System.currentTimeMillis() - Math.max(lastReadTimestamp, lastWrittenTimestamp)

    override fun getReadBytes(): Long = readByteCount

    override fun getWrittenBytes(): Long = writtenByteCount

    override fun getIdleTime(): Long = System.currentTimeMillis() - lastActiveTime

    override fun getMaxIdleTime(): Long = timeout

    override fun isClosed(): Boolean = socketChannelClosed.get()

    override fun getLocalAddress(): InetSocketAddress = socketChannel.localAddress as InetSocketAddress

    override fun getRemoteAddress(): InetSocketAddress = socketChannel.remoteAddress as InetSocketAddress

    override fun onRead(messageConsumer: Consumer<ByteBuffer>): TcpConnection {
        receivedMessageConsumer = messageConsumer
        return this
    }

    private fun launchWritingJob() = launchWithAttr(dataTransThread) {
        while (!outputShutdownState.get()) {
            when (val msg = outChannel.receive()) {
                is Buffer -> {
                    while (msg.buffer.hasRemaining()) {
                        try {
                            val count = socketChannel.aWrite(msg.buffer, timeout, TimeUnit.SECONDS)
                            msg.result.accept(Result(true, count, null))
                            if (count < 0) {
                                shutdown()
                                break
                            }
                        } catch (e: InterruptedByTimeoutException) {
                            log.warn { "Tcp connection idle timeout. $id, $idleTime" }
                            shutdown()
                            msg.result.accept(Result(false, -1, e))
                            break
                        } catch (e: Exception) {
                            log.error(e) { "Tcp connection output exception. $id" }
                            shutdown()
                            msg.result.accept(Result(false, -1, e))
                            break
                        }
                    }
                }
                is Buffers -> {
                    while (msg.hasRemaining()) {
                        try {
                            val offset = msg.getCurrentOffset()
                            val length = msg.getCurrentLength()
                            val count = socketChannel.aWrite(msg.buffers, offset, length, timeout, TimeUnit.SECONDS)
                            msg.result.accept(Result(true, count, null))
                            if (count < 0) {
                                shutdown()
                                break
                            }
                        } catch (e: InterruptedByTimeoutException) {
                            log.warn { "Tcp connection idle timeout. $id, $idleTime" }
                            shutdown()
                            msg.result.accept(Result(false, -1, e))
                            break
                        } catch (e: Exception) {
                            log.error(e) { "Tcp connection output exception. $id" }
                            shutdown()
                            msg.result.accept(Result(false, -1, e))
                            break
                        }
                    }
                }
                is BufferList -> {
                    while (msg.hasRemaining()) {
                        try {
                            val offset = msg.getCurrentOffset()
                            val length = msg.getCurrentLength()
                            val count = socketChannel.aWrite(
                                msg.bufferList.toTypedArray(),
                                offset,
                                length,
                                timeout,
                                TimeUnit.SECONDS
                                                            )
                            msg.result.accept(Result(true, count, null))
                            if (count < 0) {
                                shutdown()
                                break
                            }
                        } catch (e: InterruptedByTimeoutException) {
                            log.warn { "Tcp connection idle timeout. $id, $idleTime" }
                            shutdown()
                            msg.result.accept(Result(false, -1, e))
                            break
                        } catch (e: Exception) {
                            log.error(e) { "Tcp connection output exception. $id" }
                            shutdown()
                            msg.result.accept(Result(false, -1, e))
                            break
                        }
                    }
                }
                is Shutdown -> {
                    shutdown()
                }
            }
        }
    }

    override fun startAutomaticReading(): TcpConnection {
        if (autoReadState.compareAndSet(false, true)) {
            launchWithAttr(dataTransThread) {
                log.debug { "start automatic reading" }

                while (true) {
                    val buf = ByteBuffer.allocate(adaptiveBufferSize.getBufferSize())
                    log.debug { "current buffer size ${buf.remaining()}" }

                    try {
                        val count = socketChannel.aRead(buf, timeout, TimeUnit.SECONDS)
                        if (count < 0) {
                            log.debug { "input channel remote close. $id, $count " }
                            if (isShutdownInput && isShutdownOutput) {
                                closeNow()
                                break
                            } else {
                                shutdown()
                            }
                        } else {
                            adaptiveBufferSize.setCurrentDataSize(count)
                            readByteCount += count
                            buf.flip()
                            try {
                                receivedMessageConsumer.accept(buf)
                            } catch (e: Exception) {
                                exceptionConsumers.forEach { it.accept(e) }
                            }
                        }
                    } catch (e: InterruptedByTimeoutException) {
                        log.warn { "Tcp connection idle timeout. $id, $idleTime" }
                        shutdown()
                        break
                    } catch (e: Exception) {
                        log.warn(e) { "Tcp connection input exception. $id" }
                        shutdown()
                        break
                    }
                }
            }
        }
        return this
    }

    override fun isAutomaticReading(): Boolean = autoReadState.get()

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
            outChannel.offer(Shutdown)
        } else {
            result.accept(closeFailureResult)
        }
        return this
    }

    override fun close() {
        close(EMPTY_CONSUMER_RESULT)
    }

    private fun shutdown() {
        if (inputShutdownState.compareAndSet(false, true)) {
            try {
                socketChannel.shutdownInput()
            } catch (e: ClosedChannelException) {
                log.warn { "The channel is closed. $id" }
            } catch (e: IOException) {
                log.error { "Shutdown input exception. $id" }
            }
        } else {
            log.info { "The tcp connection has shutdown input. $id" }
        }
        if (outputShutdownState.compareAndSet(false, true)) {
            try {
                socketChannel.shutdownOutput()
            } catch (e: ClosedChannelException) {
                log.warn { "The channel is closed. $id" }
            } catch (e: IOException) {
                log.error { "Shutdown output exception. $id" }
            }
        } else {
            log.info { "The tcp connection has shutdown output. $id" }
        }
    }

    override fun isShutdownInput(): Boolean = inputShutdownState.get()

    override fun isShutdownOutput(): Boolean = outputShutdownState.get()

    override fun closeNow(): TcpConnection {
        if (socketChannelClosed.compareAndSet(false, true)) {
            try {
                closeChannel()
                socketChannel.close()
                log.info { "tcp connection close success. $id" }
                closeConsumer.accept(Result.SUCCESS)
                closeCallbacks.forEach { it.call() }
            } catch (e: Exception) {
                log.error(e) { "close socket channel exception. $id" }
            }
        }
        return this
    }

    private fun closeChannel() {
        try {
            inChannel.close()
            outChannel.close()
        } catch (e: Exception) {
            log.warn(e) { "close coroutine channel exception. $id" }
        }
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
        buffers.forEachIndexed { index, byteBuffer ->
            if (index >= offset && byteBuffer.hasRemaining()) {
                return index
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
        bufferList.forEachIndexed { index, byteBuffer ->
            if (index >= offset && byteBuffer.hasRemaining()) {
                return index
            }
        }
        return bufferList.size
    }

    fun getCurrentLength(): Int = bufferList.size - getCurrentOffset()

    fun hasRemaining(): Boolean = getCurrentOffset() < bufferList.size
}

object Shutdown : Message()