package com.fireflysource.net.tcp.secure

import com.fireflysource.common.coroutine.blocking
import com.fireflysource.common.exception.UnknownTypeException
import com.fireflysource.common.io.*
import com.fireflysource.common.io.BufferUtils.EMPTY_BUFFER
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.buffer.OutputBuffer
import com.fireflysource.net.tcp.buffer.OutputBufferList
import com.fireflysource.net.tcp.buffer.OutputBuffers
import com.fireflysource.net.tcp.buffer.OutputDataMessage
import com.fireflysource.net.tcp.secure.exception.SecureNetException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import javax.net.ssl.SSLEngineResult.HandshakeStatus.*
import javax.net.ssl.SSLEngineResult.Status.*

abstract class AbstractAsyncSecureEngine(
    private val coroutineScope: CoroutineScope,
    private val sslEngine: SSLEngine,
    private val applicationProtocolSelector: ApplicationProtocolSelector
) : SecureEngine {

    companion object {
        private val log = SystemLogger.create(AbstractAsyncSecureEngine::class.java)
    }

    private var readSupplier: Supplier<CompletableFuture<ByteBuffer>>? = null
    private var writeFunction: Function<ByteBuffer, CompletableFuture<Int>>? = null

    private var inPacketBuffer = EMPTY_BUFFER
    private val inAppBuffer = BufferUtils.allocateDirect(sslEngine.session.applicationBufferSize)
    private val outPacketBuffer = BufferUtils.allocateDirect(sslEngine.session.packetBufferSize)

    private val closed = AtomicBoolean(false)
    private var handshakeStatus = sslEngine.handshakeStatus
    private val handshakeFinished = AtomicBoolean(false)
    private val beginHandshake = AtomicBoolean(false)
    private var unwrapResultStatus: SSLEngineResult.Status = OK

    override fun onHandshakeRead(supplier: Supplier<CompletableFuture<ByteBuffer>>): SecureEngine {
        this.readSupplier = supplier
        return this
    }

    override fun onHandshakeWrite(function: Function<ByteBuffer, CompletableFuture<Int>>): SecureEngine {
        this.writeFunction = function
        return this
    }

    override fun beginHandshake(result: Consumer<Result<HandshakeResult?>>) {
        if (beginHandshake.compareAndSet(false, true)) {
            launchHandshakeJob(result)
        } else {
            result.accept(
                Result(
                    false, null,
                    SecureNetException("The handshake has begun, do not invoke it method repeatedly.")
                )
            )
        }
    }

    private fun launchHandshakeJob(result: Consumer<Result<HandshakeResult?>>) = coroutineScope.launch {
        try {
            val stashedAppBuffers = LinkedList<ByteBuffer>()
            doHandshake(stashedAppBuffers)
            result.accept(Result(true, HandshakeData(stashedAppBuffers, applicationProtocol), null))
        } catch (e: Exception) {
            result.accept(Result(false, null, e))
        }
    }

    private suspend fun doHandshake(stashedAppBuffers: MutableList<ByteBuffer>) {
        begin()
        handshakeLoop@ while (true) {
            when (handshakeStatus) {
                NEED_WRAP -> doHandshakeWrap()
                NEED_UNWRAP -> doHandshakeUnwrap(stashedAppBuffers)
                NEED_TASK -> runDelegatedTasks()
                NOT_HANDSHAKING, FINISHED -> {
                    handshakeComplete()
                    break@handshakeLoop
                }
                else -> throw UnknownTypeException("Unknown handshake status. $handshakeStatus")
            }
        }
    }

    private fun begin() {
        sslEngine.beginHandshake()
        handshakeStatus = sslEngine.handshakeStatus
        val packetBufferSize = sslEngine.session.packetBufferSize
        val applicationBufferSize = sslEngine.session.applicationBufferSize
        val status = handshakeStatus
        log.info {
            "Begin TLS handshake. mode: ${getMode()}, status: ${status}, " +
                    "packetSize: ${packetBufferSize}, appSize: $applicationBufferSize"
        }
    }

    private suspend fun doHandshakeWrap() {
        val bufferList = encrypt(EMPTY_BUFFER)
        if (bufferList.hasRemaining()) {
            val length = writeFunction?.apply(bufferList)?.await()
            log.debug { "Wrap TLS handshake data. status: $handshakeStatus, mode: ${getMode()}, length: $length" }
        }
    }

    private suspend fun doHandshakeUnwrap(stashedAppBuffers: MutableList<ByteBuffer>) {
        val receivedBuffer = when {
            unwrapResultStatus == BUFFER_UNDERFLOW -> readSupplier?.get()?.await()
            inPacketBuffer.hasRemaining() -> EMPTY_BUFFER
            else -> readSupplier?.get()?.await()
        }
        if (receivedBuffer != null) {
            val length = inPacketBuffer.remaining() + receivedBuffer.remaining()
            val inAppBuffer = decrypt(receivedBuffer)
            val remaining = inAppBuffer.remaining()
            if (remaining > 0) {
                stashedAppBuffers.add(inAppBuffer)
            }
            log.debug { "Unwrap TLS handshake data. status: $handshakeStatus, mode: ${getMode()}, length: ${length}, stashedBuffer: $remaining" }
        }
    }

    private suspend fun runDelegatedTasks() {
        while (true) {
            val runnable: Runnable? = sslEngine.delegatedTask
            if (runnable != null) {
                log.debug { "Run TLS handshake delegated tasks. status: ${sslEngine.handshakeStatus}" }
                blocking { runnable.run() }.join()
            } else break
        }
        handshakeStatus = sslEngine.handshakeStatus
        log.debug { "After run TLS handshake delegated tasks. no tasks: ${sslEngine.delegatedTask == null}, status: $handshakeStatus" }
    }

    private fun handshakeComplete() {
        if (handshakeFinished.compareAndSet(false, true)) {
            val tlsProtocol = sslEngine.session.protocol
            val cipherSuite = sslEngine.session.cipherSuite
            val inPacketBufferRemaining = inPacketBuffer.remaining()
            log.info(
                "TLS handshake success. mode: ${getMode()}, protocol: {} {}, cipher: {}, status: {}, inPacketRemaining: {}",
                applicationProtocol, tlsProtocol, cipherSuite, handshakeStatus, inPacketBufferRemaining
            )
        }
    }

    override fun encrypt(outAppBuffer: ByteBuffer): ByteBuffer =
        encryptBuffers(OutputBuffer(outAppBuffer, discard()))

    override fun encrypt(byteBuffers: Array<ByteBuffer>, offset: Int, length: Int): ByteBuffer =
        encryptBuffers(OutputBuffers(byteBuffers, offset, length, discard()))


    override fun encrypt(byteBuffers: MutableList<ByteBuffer>, offset: Int, length: Int): ByteBuffer =
        encryptBuffers(OutputBufferList(byteBuffers, offset, length, discard()))

    private fun encryptBuffers(outAppBuffer: OutputDataMessage): ByteBuffer {
        var packetBuffer = this.outPacketBuffer
        val pos = packetBuffer.flipToFill()

        fun wrap() = when (outAppBuffer) {
            is OutputBuffer -> sslEngine.wrap(outAppBuffer.buffer, packetBuffer)
            is OutputBufferList -> sslEngine.wrap(outAppBuffer.buffers, packetBuffer)
            is OutputBuffers -> sslEngine.wrap(outAppBuffer.buffers, packetBuffer)
        }

        wrap@ while (true) {
            val result = wrap()
            handshakeStatus = result.handshakeStatus

            when (result.status) {
                BUFFER_OVERFLOW -> {
                    packetBuffer = packetBuffer.addCapacity(sslEngine.session.packetBufferSize)
                    val capacity = packetBuffer.capacity()
                    val remaining = packetBuffer.remaining()
                    log.debug { "Resize out packet buffer. capacity: $capacity, remaining: $remaining" }
                }
                OK -> {
                    if (handshakeStatus != NEED_WRAP && result.bytesProduced() == 0 && result.bytesConsumed() == 0) {
                        break@wrap
                    }
                    if (!outAppBuffer.hasRemaining()) {
                        break@wrap
                    }
                }
                CLOSED -> {
                    sslEngine.closeOutbound()
                    break@wrap
                }
                else -> throw SecureNetException("Wrap data result status error. ${result.status}")
            }

        }

        return packetBuffer.flipToFlush(pos).copy().also { BufferUtils.clear(this.outPacketBuffer) }
    }

    override fun decrypt(receivedBuffer: ByteBuffer): ByteBuffer {
        merge(receivedBuffer)

        if (!inPacketBuffer.hasRemaining()) {
            return EMPTY_BUFFER
        }

        var appBuffer = this.inAppBuffer
        val pos = appBuffer.flipToFill()

        unwrap@ while (true) {
            val result = sslEngine.unwrap(inPacketBuffer, appBuffer)
            handshakeStatus = result.handshakeStatus
            unwrapResultStatus = result.status

            when (result.status) {
                BUFFER_UNDERFLOW -> {
                    if (inPacketBuffer.remaining() < sslEngine.session.packetBufferSize) {
                        break@unwrap
                    }
                }
                BUFFER_OVERFLOW -> {
                    appBuffer = appBuffer.addCapacity(sslEngine.session.applicationBufferSize)
                    val capacity = appBuffer.capacity()
                    val remaining = appBuffer.remaining()
                    log.debug { "Resize in app buffer. capacity: $capacity, remaining: $remaining" }
                }
                OK -> {
                    if (handshakeStatus != NEED_UNWRAP && result.bytesProduced() == 0 && result.bytesConsumed() == 0) {
                        break@unwrap
                    }
                    if (!inPacketBuffer.hasRemaining()) {
                        inPacketBuffer = EMPTY_BUFFER
                        break@unwrap
                    }
                }
                CLOSED -> {
                    sslEngine.closeInbound()
                    break@unwrap
                }
                else -> throw SecureNetException("Unwrap packets state exception. ${result.status}")
            }
        }

        return appBuffer.flipToFlush(pos).copy().also { BufferUtils.clear(this.inAppBuffer) }
    }

    private fun merge(receivedBuffer: ByteBuffer) {
        if (!receivedBuffer.hasRemaining()) {
            return
        }

        inPacketBuffer = if (inPacketBuffer.hasRemaining()) {
            log.debug {
                "Merge received packet buffer. mode: ${getMode()}, " +
                        "in packet buffer: ${inPacketBuffer.remaining()}, " +
                        "received buffer: ${receivedBuffer.remaining()}"
            }

            val capacity = inPacketBuffer.remaining() + receivedBuffer.remaining()
            BufferUtils.allocate(capacity).append(inPacketBuffer).append(receivedBuffer)
        } else {
            receivedBuffer
        }
    }

    private fun getMode(): String = if (isClientMode) "Client" else "Server"

    override fun isClientMode(): Boolean = sslEngine.useClientMode

    override fun getSupportedApplicationProtocols(): MutableList<String> =
        applicationProtocolSelector.supportedApplicationProtocols

    override fun getApplicationProtocol(): String = applicationProtocolSelector.applicationProtocol

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            sslEngine.closeOutbound()
        }
    }

    override fun isHandshakeComplete(): Boolean = handshakeFinished.get()

    private class HandshakeData(
        private val stashedAppBuffers: MutableList<ByteBuffer>,
        private val applicationProtocol: String
    ) : HandshakeResult {
        override fun getApplicationProtocol(): String = applicationProtocol
        override fun getStashedAppBuffers(): MutableList<ByteBuffer> = stashedAppBuffers
    }

}