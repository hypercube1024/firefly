package com.fireflysource.net.tcp.secure

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.coroutine.launchBlocking
import com.fireflysource.common.io.*
import com.fireflysource.common.io.BufferUtils.EMPTY_BUFFER
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
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
    private var writeFunction: Function<List<ByteBuffer>, CompletableFuture<Long>>? = null

    private var inPacketBuffer = EMPTY_BUFFER

    private val closed = AtomicBoolean(false)
    private var handshakeStatus = sslEngine.handshakeStatus
    private val handshakeFinished = AtomicBoolean(false)
    private val beginHandshake = AtomicBoolean(false)

    override fun onHandshakeRead(supplier: Supplier<CompletableFuture<ByteBuffer>>): SecureEngine {
        this.readSupplier = supplier
        return this
    }

    override fun onHandshakeWrite(function: Function<List<ByteBuffer>, CompletableFuture<Long>>): SecureEngine {
        this.writeFunction = function
        return this
    }

    override fun beginHandshake(result: Consumer<Result<Void?>>) {
        if (beginHandshake.compareAndSet(false, true)) {
            launchHandshakeJob(result)
        } else {
            result.accept(Result.createFailedResult(SecureNetException("The handshake has begun, do not invoke it method repeatedly.")))
        }
    }

    private fun launchHandshakeJob(result: Consumer<Result<Void?>>) = coroutineScope.launch {
        try {
            doHandshake()
            result.accept(Result.createSuccessResult())
        } catch (e: Exception) {
            result.accept(Result.createFailedResult(e))
        }
    }

    private suspend fun doHandshake() {
        begin()
        handshakeLoop@ while (true) {
            when (handshakeStatus) {
                NEED_WRAP -> doHandshakeWrap()
                NEED_UNWRAP -> doHandshakeUnwrap()
                NEED_TASK -> runDelegatedTasks()
                NOT_HANDSHAKING, FINISHED -> {
                    handshakeComplete()
                    break@handshakeLoop
                }
                else -> throw SecureNetException("Handshake state exception. $handshakeStatus")
            }
        }
    }

    private fun begin() {
        sslEngine.beginHandshake()
        handshakeStatus = sslEngine.handshakeStatus
        val packetBufferSize = sslEngine.session.packetBufferSize
        val applicationBufferSize = sslEngine.session.applicationBufferSize
        log.info {
            "Begin TLS handshake. mode: ${getMode()}, status: ${handshakeStatus}, " +
                    "packetSize: ${packetBufferSize}, appSize: $applicationBufferSize"
        }
    }

    private suspend fun doHandshakeWrap() {
        val bufferList = encrypt(EMPTY_BUFFER)
        if (bufferList.isNotEmpty()) {
            val length = writeFunction?.apply(bufferList)?.await()
            log.info { "Write TLS handshake data. mode: ${getMode()}, length: $length" }
        }
    }

    private suspend fun doHandshakeUnwrap() {
        val receivedBuffer = readSupplier?.get()?.await()
        if (receivedBuffer != null) {
            val length = receivedBuffer.remaining()
            log.info { "Receive TLS handshake data. mode: ${getMode()}, length: $length" }
            decrypt(receivedBuffer)
        }
    }

    private suspend fun runDelegatedTasks() {
        // Conscrypt delegated tasks are always null
        var runnable: Runnable
        while (sslEngine.delegatedTask.also { runnable = it } != null) {
            launchBlocking { runnable.run() }.join()
        }
        handshakeStatus = sslEngine.handshakeStatus
    }

    private fun handshakeComplete() {
        if (handshakeFinished.compareAndSet(false, true)) {
            val tlsProtocol = sslEngine.session.protocol
            val cipherSuite = sslEngine.session.cipherSuite
            val inPacketBufferRemaining = inPacketBuffer.remaining()
            Assert.isTrue(inPacketBufferRemaining == 0, "Received handshake data must be not remaining")
            log.info(
                "TLS handshake success. mode: ${getMode()}, protocol: {} {}, cipher: {}, status: {}, inPacketRemaining: {}",
                applicationProtocol, tlsProtocol, cipherSuite, handshakeStatus, inPacketBufferRemaining
            )
        }
    }

    override fun encrypt(outAppBuffer: ByteBuffer): MutableList<ByteBuffer> {
        val outPacketBuffers = LinkedList<ByteBuffer>()
        var outPacketBuffer = BufferUtils.allocate(sslEngine.session.packetBufferSize)
        val pos = outPacketBuffer.flipToFill()

        wrap@ while (true) {
            val result = sslEngine.wrap(outAppBuffer, outPacketBuffer)
            handshakeStatus = result.handshakeStatus
            when (result.status) {
                BUFFER_OVERFLOW -> {
                    outPacketBuffer = resizePacketBuffer(outPacketBuffer)
                }
                OK -> {
                    outPacketBuffer.flipToFlush(pos)
                    if (outPacketBuffer.hasRemaining()) {
                        outPacketBuffers.add(outPacketBuffer.duplicate())
                    }
                    if (!outAppBuffer.hasRemaining()) {
                        break@wrap
                    }
                }
                CLOSED -> sslEngine.closeOutbound()
                else -> throw SecureNetException("Wrap app data state exception. ${result.status}")
            }
        }
        return outPacketBuffers
    }

    override fun decrypt(receivedBuffer: ByteBuffer): ByteBuffer {
        merge(receivedBuffer)

        if (!inPacketBuffer.hasRemaining()) {
            return EMPTY_BUFFER
        }

        var inAppBuffer = BufferUtils.allocate(sslEngine.session.packetBufferSize)
        val pos = inAppBuffer.flipToFill()
        unwrap@ while (true) {
            val result = sslEngine.unwrap(inPacketBuffer, inAppBuffer)
            handshakeStatus = result.handshakeStatus
            when (result.status) {
                BUFFER_UNDERFLOW -> {
                    if (inPacketBuffer.remaining() < sslEngine.session.packetBufferSize) {
                        break@unwrap
                    }
                }
                BUFFER_OVERFLOW -> {
                    inAppBuffer = resizeInAppBuffer(inAppBuffer)
                }
                OK -> {
                    if (!inPacketBuffer.hasRemaining()) {
                        break@unwrap
                    }
                }
                CLOSED -> sslEngine.closeInbound()
                else -> throw SecureNetException("Unwrap packets state exception. ${result.status}")
            }
        }
        inAppBuffer.flipToFlush(pos)
        return inAppBuffer
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

    private fun resizeInAppBuffer(appBuffer: ByteBuffer): ByteBuffer {
        return appBuffer.addCapacity(sslEngine.session.applicationBufferSize);
    }

    private fun resizePacketBuffer(packetBuffer: ByteBuffer): ByteBuffer {
        return packetBuffer.addCapacity(sslEngine.session.packetBufferSize)
    }

    private fun getMode(): String = if (isClientMode) "Client" else "Server"

    override fun isClientMode(): Boolean = sslEngine.useClientMode

    override fun getSupportedApplicationProtocols(): MutableList<String> =
        applicationProtocolSelector.supportedApplicationProtocols

    override fun getApplicationProtocol(): String = applicationProtocolSelector.applicationProtocol

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            sslEngine.closeOutbound()
            sslEngine.closeInbound()
        }
    }

    override fun isHandshakeComplete(): Boolean = handshakeFinished.get()

}