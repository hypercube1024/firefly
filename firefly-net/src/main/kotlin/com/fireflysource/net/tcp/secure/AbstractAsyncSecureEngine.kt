package com.fireflysource.net.tcp.secure

import com.fireflysource.common.coroutine.launchBlocking
import com.fireflysource.common.io.*
import com.fireflysource.common.io.BufferUtils.EMPTY_BUFFER
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.aio.AdaptiveBufferSize
import com.fireflysource.net.tcp.buffer.Buffer
import com.fireflysource.net.tcp.buffer.BufferList
import com.fireflysource.net.tcp.buffer.Buffers
import com.fireflysource.net.tcp.buffer.OutputMessage
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
    private val inAppAdaptiveBufferSize = AdaptiveBufferSize()
    private val outPacketAdaptiveBufferSize = AdaptiveBufferSize()

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
        val status = handshakeStatus
        log.info {
            "Begin TLS handshake. mode: ${getMode()}, status: ${status}, " +
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
            log.info(
                "TLS handshake success. mode: ${getMode()}, protocol: {} {}, cipher: {}, status: {}, inPacketRemaining: {}",
                applicationProtocol, tlsProtocol, cipherSuite, handshakeStatus, inPacketBufferRemaining
            )
        }
    }

    private fun allocateInApp(): ByteBuffer {
        val size = inAppAdaptiveBufferSize.getBufferSize()
        log.debug { "Allocate in app buffer size: $size" }
        return BufferUtils.allocate(size)
    }

    private fun updateInAppBufferSize(size: Int) {
        inAppAdaptiveBufferSize.update(size)
        log.debug { "In app size: $size" }
    }

    private fun allocateOutPacket(): ByteBuffer {
        val size = outPacketAdaptiveBufferSize.getBufferSize()
        log.debug { "Allocate out packet buffer size: $size" }
        return BufferUtils.allocate(size)
    }

    private fun updateOutPacketBufferSize(size: Int) {
        outPacketAdaptiveBufferSize.update(size)
        log.debug { "Out packet size: $size" }
    }

    override fun encrypt(outAppBuffer: ByteBuffer): List<ByteBuffer> = encryptBuffers(Buffer(outAppBuffer, discard()))

    override fun encrypt(byteBuffers: Array<ByteBuffer>, offset: Int, length: Int): List<ByteBuffer> {
        return encryptBuffers(Buffers(byteBuffers, offset, length, discard()))
    }

    override fun encrypt(byteBuffers: MutableList<ByteBuffer>, offset: Int, length: Int): List<ByteBuffer> {
        return encryptBuffers(BufferList(byteBuffers, offset, length, discard()))
    }

    private fun encryptBuffers(outAppBuffer: OutputMessage): List<ByteBuffer> {
        val outPacketBuffers = LinkedList<ByteBuffer>()
        var outPacketBuffer = allocateOutPacket()
        var pos = outPacketBuffer.flipToFill()

        fun wrap() = when (outAppBuffer) {
            is Buffer -> sslEngine.wrap(outAppBuffer.buffer, outPacketBuffer)
            is Buffers -> sslEngine.wrap(outAppBuffer.buffers, outPacketBuffer)
            is BufferList -> sslEngine.wrap(outAppBuffer.buffers, outPacketBuffer)
            else -> throw IllegalArgumentException("Out app buffer type error.")
        }

        wrap@ while (true) {
            val result = wrap()
            handshakeStatus = result.handshakeStatus
            when (result.status) {
                BUFFER_OVERFLOW -> {
                    outPacketBuffer = outPacketBuffer.addCapacity(sslEngine.session.packetBufferSize)
                    val size = outPacketBuffer.capacity()
                    log.debug { "Out packet buffer adds capacity: $size" }
                }
                OK -> {
                    outPacketBuffer.flipToFlush(pos)
                    if (outPacketBuffer.hasRemaining()) {
                        outPacketBuffers.add(outPacketBuffer)
                    }

                    if (outAppBuffer.hasRemaining()) {
                        outPacketBuffer = allocateOutPacket()
                        pos = outPacketBuffer.flipToFill()
                    } else {
                        break@wrap
                    }
                }
                CLOSED -> {
                    sslEngine.closeOutbound()
                    break@wrap
                }
                else -> throw SecureNetException("Wrap app data state exception. ${result.status}")
            }
        }

        val size = outPacketBuffers.sumBy { it.remaining() }
        updateOutPacketBufferSize(size)
        return outPacketBuffers
    }

    override fun decrypt(receivedBuffer: ByteBuffer): ByteBuffer {
        merge(receivedBuffer)

        if (!inPacketBuffer.hasRemaining()) {
            return EMPTY_BUFFER
        }

        var inAppBuffer = allocateInApp()
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
                    inAppBuffer = inAppBuffer.addCapacity(sslEngine.session.applicationBufferSize)
                    val size = inAppBuffer.capacity()
                    log.debug { "In app buffer adds capacity: $size" }
                }
                OK -> {
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

        inAppBuffer.flipToFlush(pos)
        updateInAppBufferSize(inAppBuffer.remaining())
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