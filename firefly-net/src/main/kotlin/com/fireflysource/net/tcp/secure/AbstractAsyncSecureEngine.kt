package com.fireflysource.net.tcp.secure

import com.fireflysource.common.coroutine.CoroutineDispatchers.ioBlocking
import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.BufferUtils.EMPTY_BUFFER
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.secure.exception.SecureNetException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult.HandshakeStatus.*
import javax.net.ssl.SSLEngineResult.Status.*

abstract class AbstractAsyncSecureEngine(
    private val tcpConnection: TcpConnection,
    private val sslEngine: SSLEngine,
    private val applicationProtocolSelector: ApplicationProtocolSelector
) : SecureEngine {

    companion object {
        private val log = SystemLogger.create(AbstractAsyncSecureEngine::class.java)
    }

    private var inPacketBuffer = EMPTY_BUFFER

    private val closed = AtomicBoolean(false)
    private var handshakeStatus = sslEngine.handshakeStatus
    private val handshakeFinished = AtomicBoolean(false)
    private val beginHandshake = AtomicBoolean(false)

    override fun beginHandshake(result: Consumer<Result<Void?>>) {
        if (beginHandshake.compareAndSet(false, true)) {
            tcpConnection.startReading()
            startTlsJob(result)
        } else {
            result.accept(Result.createFailedResult(SecureNetException("The handshake has begun, do not invoke it method repeatedly.")))
        }
    }

    override fun isClientMode(): Boolean = sslEngine.useClientMode

    private fun startTlsJob(result: Consumer<Result<Void?>>) = tcpConnection.coroutineScope.launch {
        try {
            initHandshakeStatus()
            doHandshake()
            result.accept(Result.createSuccessResult())
        } catch (e: Exception) {
            result.accept(Result.createFailedResult(e))
        }
    }

    private fun initHandshakeStatus() {
        handshakeStatus = if (isClientMode) {
            NEED_WRAP
        } else {
            NEED_UNWRAP
        }
    }

    private suspend fun doHandshake() {
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

    private suspend fun doHandshakeWrap() {
        val bufferList = encrypt(EMPTY_BUFFER)
        if (bufferList.isNotEmpty()) {
            tcpConnection.write(bufferList, 0, bufferList.size).await()
        }
    }

    private suspend fun doHandshakeUnwrap() {
        val receivedBuffer = tcpConnection.inputChannel.receive()
        decrypt(receivedBuffer)
    }

    private suspend fun runDelegatedTasks() {
        // Conscrypt delegated tasks are always null
        var runnable: Runnable
        while (sslEngine.delegatedTask.also { runnable = it } != null) {
            launchGlobally(ioBlocking) { runnable.run() }.join()
        }
        handshakeStatus = sslEngine.handshakeStatus
    }

    private fun handshakeComplete() {
        if (handshakeFinished.compareAndSet(false, true)) {
            val tlsProtocol = sslEngine.session.protocol
            val cipherSuite = sslEngine.session.cipherSuite
            log.info(
                "Connection handshake success. id: {}, protocol: {} {}, cipher: {}",
                tcpConnection.id, applicationProtocol, tlsProtocol, cipherSuite
            )
        }
    }


    private fun newBuffer(size: Int) = ByteBuffer.allocate(size)

    private fun newAppBuffer(): ByteBuffer {
        return ByteBuffer.allocate(sslEngine.session.applicationBufferSize)
    }

    private fun newPacketBuffer(): ByteBuffer {
        return ByteBuffer.allocate(sslEngine.session.packetBufferSize)
    }

    override fun encrypt(outAppBuffer: ByteBuffer): MutableList<ByteBuffer> {
        val outPacketBuffers = LinkedList<ByteBuffer>()
        var outPacketBuffer = newPacketBuffer()

        wrap@ while (true) {
            val result = sslEngine.wrap(outAppBuffer, outPacketBuffer)
            handshakeStatus = result.handshakeStatus
            when (result.status) {
                BUFFER_OVERFLOW -> {
                    outPacketBuffer = resizePacketBuffer(outPacketBuffer)
                }
                OK -> {
                    outPacketBuffer.flip()
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

        var inAppBuffer = newAppBuffer()
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
        inAppBuffer.flip()
        return inAppBuffer
    }

    private fun merge(receivedBuffer: ByteBuffer) {
        if (!receivedBuffer.hasRemaining()) {
            return
        }
        inPacketBuffer = if (inPacketBuffer.hasRemaining()) {
            log.debug {
                "Connection merge received packet buffer. " +
                        "id: ${tcpConnection.id}, " +
                        "in packet buffer: ${inPacketBuffer.remaining()}, " +
                        "received buffer: ${receivedBuffer.remaining()}"
            }
            val buffer = newBuffer(inPacketBuffer.remaining() + receivedBuffer.remaining())
            buffer.put(inPacketBuffer).put(receivedBuffer).flip()
            buffer
        } else {
            receivedBuffer
        }
    }

    private fun resizeInAppBuffer(appBuffer: ByteBuffer): ByteBuffer {
        return BufferUtils.addCapacity(appBuffer, sslEngine.session.applicationBufferSize);
    }

    private fun resizePacketBuffer(packetBuffer: ByteBuffer): ByteBuffer {
        return BufferUtils.addCapacity(packetBuffer, sslEngine.session.packetBufferSize)
    }

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