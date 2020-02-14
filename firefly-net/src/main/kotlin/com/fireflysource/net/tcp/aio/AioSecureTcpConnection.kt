package com.fireflysource.net.tcp.aio

import com.fireflysource.common.sys.Result
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.buffer.*
import com.fireflysource.net.tcp.secure.SecureEngine
import kotlinx.coroutines.channels.Channel
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

    private fun encryptMessageAndFlush(outputMessage: OutputMessage) {
        when (outputMessage) {
            is OutputBuffer -> encryptAndFlushBuffer(outputMessage)
            is OutputBuffers -> encryptAndFlushBuffers(outputMessage)
            is OutputBufferList -> encryptAndFlushBuffers(outputMessage)
            is ShutdownOutput -> tcpConnection.close(outputMessage.result)
        }
    }

    private fun encryptAndFlushBuffers(outputMessage: OutputBuffers) {
        val encryptedBuffers = secureEngine.encrypt(
            outputMessage.buffers,
            outputMessage.getCurrentOffset(),
            outputMessage.getCurrentLength()
        )
        tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size, outputMessage.result)
    }

    private fun encryptAndFlushBuffer(outputMessage: OutputBuffer) {
        val encryptedBuffers = secureEngine.encrypt(outputMessage.buffer)
        tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size) {
            outputMessage.result.accept(Result(it.isSuccess, it.value.toInt(), it.throwable))
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