package com.fireflysource.net.tcp.aio

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.coroutine.consumeAll
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.WrappedTcpConnection
import com.fireflysource.net.tcp.buffer.*
import com.fireflysource.net.tcp.secure.SecureEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
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
) : TcpConnection by tcpConnection, WrappedTcpConnection {

    companion object {
        private val log = SystemLogger.create(AioSecureTcpConnection::class.java)
    }

    private val encryptedOutChannel: Channel<OutputMessage> = Channel(Channel.UNLIMITED)
    private val stashedBuffers = LinkedList<ByteBuffer>()
    private val beginHandshake = AtomicBoolean(false)

    init {
        secureEngine
            .onHandshakeWrite { tcpConnection.write(it) }
            .onHandshakeRead { tcpConnection.read() }
        tcpConnection.onClose { secureEngine.close() }
    }

    override fun getRawTcpConnection(): TcpConnection = tcpConnection

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
        while (true) {
            when (val message = encryptedOutChannel.receive()) {
                is OutputBuffer -> encryptAndFlushBuffer(message)
                is OutputBuffers -> encryptAndFlushBuffers(message)
                is OutputBufferList -> encryptAndFlushBuffers(message)
                is ShutdownOutput -> {
                    shutdownOutput(message)
                    break
                }
            }
        }
    }.invokeOnCompletion { cause ->
        val e = cause ?: ClosedChannelException()
        encryptedOutChannel.consumeAll { message ->
            when (message) {
                is OutputBuffer -> message.result.accept(Result.createFailedResult(-1, e))
                is OutputBuffers -> message.result.accept(Result.createFailedResult(-1, e))
                is OutputBufferList -> message.result.accept(Result.createFailedResult(-1, e))
                is ShutdownOutput -> message.result.accept(Result.createFailedResult(e))
                else -> {
                }
            }
        }
    }

    private suspend fun encryptAndFlushBuffers(outputMessage: OutputBuffers) {
        val result = outputMessage.result
        try {
            val remaining = outputMessage.remaining()
            val buffers = outputMessage.buffers
            val offset = outputMessage.getCurrentOffset()
            val length = outputMessage.getCurrentLength()
            val encryptedBuffer = secureEngine.encrypt(buffers, offset, length)
            val size = encryptedBuffer.remaining()
            log.debug { "Encrypt and flush buffer. id: $id, src: $remaining, desc: $size, offset: $offset, length: $length" }

            if (remaining == 0L || size == 0) {
                result.accept(Result(true, 0, null))
            } else {
                tcpConnection.write(encryptedBuffer).await()
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
            val encryptedBuffer = secureEngine.encrypt(buffer)
            val size = encryptedBuffer.remaining()
            log.debug { "Encrypt and flush buffer. id: $id, src: $remaining, desc: $size" }

            if (remaining == 0 || size == 0) {
                result.accept(Result(true, 0, null))
            } else {
                tcpConnection.write(encryptedBuffer).await()
                result.accept(Result(true, remaining, null))
            }
        } catch (e: Exception) {
            result.accept(Result(false, -1, e))
        }
    }

    private fun shutdownOutput(message: ShutdownOutput) {
        tcpConnection.close(message.result)
    }

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        encryptedOutChannel.trySend(OutputBuffer(byteBuffer, result))
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        encryptedOutChannel.trySend(OutputBuffers(byteBuffers, offset, length, result))
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
    ): TcpConnection {
        encryptedOutChannel.trySend(OutputBufferList(byteBufferList, offset, length, result))
        return this
    }

    override fun close(result: Consumer<Result<Void>>): TcpConnection {
        encryptedOutChannel.trySend(ShutdownOutput(result))
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
                }
                .exceptionallyAccept { result.accept(Result(false, "", it)) }
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