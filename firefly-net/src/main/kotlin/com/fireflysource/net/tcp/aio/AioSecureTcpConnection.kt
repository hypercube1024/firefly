package com.fireflysource.net.tcp.aio

import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.buffer.*
import com.fireflysource.net.tcp.secure.SecureEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
class AioSecureTcpConnection(
    private val tcpConnection: TcpConnection,
    private val secureEngine: SecureEngine
) : TcpConnection by tcpConnection {

    companion object {
        private val log = SystemLogger.create(AioSecureTcpConnection::class.java)
    }

    private val encryptedOutChannel: Channel<OutputMessage> = Channel(Channel.UNLIMITED)
    private val stashedBuffers = LinkedList<ByteBuffer>()
    private val beginHandshake = AtomicBoolean(false)

    init {
        secureEngine
            .onHandshakeWrite { tcpConnection.write(it, 0, it.size) }
            .onHandshakeRead { tcpConnection.read() }
        tcpConnection.onClose { secureEngine.close() }
    }

    override fun read(): CompletableFuture<ByteBuffer> {
        if (!beginHandshake.get()) {
            val future = CompletableFuture<ByteBuffer>()
            future.completeExceptionally(IllegalStateException("The TLS handshake has not begun"))
            return future
        }

        val stashedBuf: ByteBuffer? = stashedBuffers.poll()
        return if (stashedBuf != null) {
            val future = CompletableFuture<ByteBuffer>()
            future.complete(stashedBuf)
            future
        } else {
            tcpConnection.read().thenApply(secureEngine::decrypt)
        }
    }

    private fun launchEncryptingAndFlushJob() = tcpConnection.coroutineScope.launch {
        while (!isShutdownOutput) {
            encryptMessageAndFlush(encryptedOutChannel.receive())
        }
    }

    private suspend fun encryptMessageAndFlush(outputMessage: OutputMessage) {
        when (outputMessage) {
            is OutputBuffer -> encryptAndFlushBuffer(outputMessage)
            is OutputBuffers -> encryptAndFlushBuffers(outputMessage)
            is OutputBufferList -> encryptAndFlushBuffers(outputMessage)
            is ShutdownOutput -> shutdownOutput(outputMessage)
            else -> throw IllegalStateException("The output message type error")
        }
    }

    private suspend fun encryptAndFlushBuffers(outputMessage: OutputBuffers) {
        val result = outputMessage.result
        try {
            val remaining = outputMessage.remaining()
            val buffers = outputMessage.buffers
            val offset = outputMessage.getCurrentOffset()
            val length = outputMessage.getCurrentLength()
            val encryptedBuffers = secureEngine.encrypt(buffers, offset, length)
            val size = encryptedBuffers.size
            log.debug { "Encrypt and flush buffer. id: $id, remaining: $remaining, size: $size, offset: $offset, length: $length" }

            if (remaining == 0L || size == 0) {
                result.accept(Result(true, 0, null))
            } else {
                tcpConnection.write(encryptedBuffers, 0, size).await()
                result.accept(Result(true, remaining, null))
            }
        } catch (e: Exception) {
            result.accept(Result(false, -1, e))
        }
    }

    private suspend fun encryptAndFlushBuffer(outputMessage: OutputBuffer) {
        val (buffer, result) = outputMessage
        try {
            val remaining = buffer.remaining()
            val encryptedBuffers = secureEngine.encrypt(buffer)
            val size = encryptedBuffers.size
            log.debug { "Encrypt and flush buffer. id: $id, remaining: $remaining, size: $size" }

            if (remaining == 0 || size == 0) {
                result.accept(Result(true, 0, null))
            } else {
                tcpConnection.write(encryptedBuffers, 0, size).await()
                result.accept(Result(true, remaining, null))
            }
        } catch (e: Exception) {
            result.accept(Result(false, -1, e))
        }
    }

    private suspend fun shutdownOutput(message: ShutdownOutput) {
        try {
            tcpConnection.closeFuture().await()
            message.result.accept(Result.SUCCESS)
        } catch (e: Exception) {
            message.result.accept(Result.createFailedResult(e))
        }
    }

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        encryptedOutChannel.offer(OutputBuffer(byteBuffer, result))
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        encryptedOutChannel.offer(OutputBuffers(byteBuffers, offset, length, result))
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        encryptedOutChannel.offer(OutputBufferList(byteBufferList, offset, length, result))
        return this
    }

    override fun close(result: Consumer<Result<Void>>): TcpConnection {
        encryptedOutChannel.offer(ShutdownOutput(result))
        return this
    }

    override fun isSecureConnection(): Boolean = true

    override fun isClientMode(): Boolean {
        return secureEngine.isClientMode
    }

    override fun isHandshakeComplete(): Boolean = secureEngine.isHandshakeComplete

    override fun beginHandshake(result: Consumer<Result<String>>): TcpConnection {
        if (beginHandshake.compareAndSet(false, true)) {
            secureEngine.beginHandshake()
                .thenAccept {
                    result.accept(Result(true, it.applicationProtocol, null))
                    it.stashedAppBuffers.forEach { b -> stashedBuffers.add(b) }
                    launchEncryptingAndFlushJob()
                }.exceptionally { e ->
                    result.accept(Result(false, "", e))
                    null
                }
        } else {
            result.accept(Result(false, "", IllegalStateException("The handshake has begun")))
        }
        return this
    }

    override fun getSupportedApplicationProtocols(): List<String> {
        return secureEngine.supportedApplicationProtocols
    }

    override fun getApplicationProtocol(): String {
        return secureEngine.applicationProtocol
    }
}