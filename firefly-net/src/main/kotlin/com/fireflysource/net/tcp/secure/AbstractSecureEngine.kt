package com.fireflysource.net.tcp.secure

import com.fireflysource.common.sys.CommonLogger
import com.fireflysource.net.tcp.Result
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.secure.exception.SecureNetException
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import javax.net.ssl.SSLEngineResult.HandshakeStatus.*
import javax.net.ssl.SSLEngineResult.Status.*
import javax.net.ssl.SSLException


/**
 * @author Pengtao Qiu
 */
abstract class AbstractSecureEngine(
    val sslEngine: SSLEngine,
    val tcpConnection: TcpConnection
                                   ) : SecureEngine {

    companion object {
        private val log = CommonLogger.create(AbstractSecureEngine::class.java)
        private val hsBuffer = ByteBuffer.allocateDirect(0)
        private val emptyBuf = ByteBuffer.allocate(0)
        private val emptyHsRet: Consumer<Result<SecureEngine>> = Consumer {}
    }

    protected val closed = AtomicBoolean(false)
    protected var initialHSComplete = AtomicBoolean(false)
    protected lateinit var initialHSStatus: SSLEngineResult.HandshakeStatus
    protected var receivedPacketBuf: ByteBuffer = emptyBuf
    protected var receivedAppBuf: ByteBuffer = emptyBuf
    protected var handshakeResult: Consumer<Result<SecureEngine>> = emptyHsRet

    override fun beginHandshake(result: Consumer<Result<SecureEngine>>) {
        handshakeResult = result
        this.sslEngine.beginHandshake()
        initialHSStatus = sslEngine.handshakeStatus
        if (sslEngine.useClientMode) {
            doHandshakeResponse()
        }
    }

    override fun decode(byteBuffer: ByteBuffer): ByteBuffer {
        if (!doHandshake(byteBuffer)) {
            return emptyBuf
        }

        if (!initialHSComplete.get()) {
            throw SecureNetException("The handshake is not complete")
        }

        merge(byteBuffer)
        if (!receivedPacketBuf.hasRemaining()) {
            return emptyBuf
        }

        needIO@ while (true) {
            val result = unwrap()
            log.debug {
                "read data result. id: ${tcpConnection.id}" +
                        "ret: $result receivedPacketBuf: ${receivedPacketBuf.remaining()}, " +
                        "appBufSize: ${receivedAppBuf.remaining()}"
            }

            when (result.status) {
                BUFFER_OVERFLOW -> {
                    resizeAppBuffer()
                    // retry the operation.
                }
                BUFFER_UNDERFLOW -> {
                    if (receivedPacketBuf.remaining() < sslEngine.session.packetBufferSize) {
                        break@needIO
                    }
                }
                OK -> {
                    if (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                        doTasks()
                    }
                    if (!receivedPacketBuf.hasRemaining()) {
                        break@needIO
                    }
                }
                CLOSED -> {
                    log.info { "read data failure. SSLEngine will close inbound. id: ${tcpConnection.id}" }
                    closeInbound()
                    break@needIO
                }

                else -> throw SecureNetException("read data exception. id: ${tcpConnection.id}, status: ${result.status}")
            }
        }

        return getCurrentReceivedAppBuf()
    }

    override fun encode(byteBuffer: ByteBuffer): List<ByteBuffer> {
        if (!byteBuffer.hasRemaining()) {
            return emptyList()
        }

        val remain = byteBuffer.remaining()
        var packetBufferSize = sslEngine.session.packetBufferSize
        val pocketBuffers = ArrayList<ByteBuffer>()
        var bytesConsumed = 0

        outer@ while (bytesConsumed < remain) {
            var packetBuffer = newBuffer(packetBufferSize)

            wrap@ while (true) {
                val result = wrap(byteBuffer, packetBuffer)
                bytesConsumed += result.bytesConsumed()

                when (result.status) {
                    OK -> {
                        if (result.handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                            doTasks()
                        }

                        packetBuffer.flip()
                        if (packetBuffer.hasRemaining()) {
                            pocketBuffers.add(packetBuffer)
                        }
                        break@wrap
                    }

                    BUFFER_OVERFLOW -> {
                        packetBufferSize = sslEngine.session.packetBufferSize
                        val b = newBuffer(packetBuffer.position() + packetBufferSize)
                        packetBuffer.flip()
                        b.put(packetBuffer)
                        packetBuffer = b
                    }

                    CLOSED -> {
                        log.info { "SSLEngine will close. id: ${tcpConnection.id}" }
                        packetBuffer.flip()
                        if (packetBuffer.hasRemaining()) {
                            pocketBuffers.add(packetBuffer)
                        }
                        break@outer
                    }

                    else -> {
                        throw SecureNetException(
                            "SSLEngine writes data exception. " +
                                    "id: ${tcpConnection.id}," +
                                    " status: ${result.status}"
                                                )
                    }
                }// retry the operation.
            }
        }
        return pocketBuffers
    }

    /**
     * The initial handshake is a procedure by which the two peers exchange
     * communication parameters until an SecureSession is established. Application
     * data can not be sent during this phase.
     *
     * @param receiveBuffer Encrypted message
     * @return True means handshake success
     * @throws IOException The I/O exception
     */
    protected fun doHandshake(receiveBuffer: ByteBuffer): Boolean {
        try {
            if (tcpConnection.isClosed) {
                close()
                completeHandshake(Result(false, this, SecureNetException("the tcp connection is closed")))
                return false
            }

            if (initialHSComplete.get()) {
                return true
            }

            when (initialHSStatus) {
                NOT_HANDSHAKING, FINISHED -> {
                    completeHandshake(Result(true, this, null))
                    return initialHSComplete.get()
                }

                NEED_UNWRAP -> {
                    doHandshakeReceive(receiveBuffer)
                    if (initialHSStatus == NEED_WRAP) {
                        doHandshakeResponse()
                    }
                }

                NEED_WRAP -> doHandshakeResponse()

                // NEED_TASK
                else -> {
                    val ex = SecureNetException("Invalid Handshaking State. $initialHSStatus")
                    completeHandshake(Result(false, this, ex))
                    throw ex
                }
            }
            return initialHSComplete.get()
        } catch (ex: IOException) {
            completeHandshake(Result(false, this, ex))
            throw ex
        }
    }

    protected fun doHandshakeReceive(receiveBuffer: ByteBuffer) {
        merge(receiveBuffer)
        needIO@ while (initialHSStatus === SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
            unwrap@ while (true) {
                val result = unwrap()
                initialHSStatus = result.handshakeStatus

                log.debug {
                    "handshake result. id: ${tcpConnection.id}, " +
                            "ret: $result initHsStat: $initialHSStatus, " +
                            "inBuf -> ${receivedPacketBuf.remaining()}"
                }

                when (result.status) {
                    SSLEngineResult.Status.OK -> {
                        when (initialHSStatus) {
                            NEED_TASK -> {
                                initialHSStatus = doTasks()
                                break@unwrap
                            }
                            NOT_HANDSHAKING, FINISHED -> {
                                completeHandshake(Result(true, this, null))
                                break@needIO
                            }
                            else -> break@unwrap
                        }
                    }

                    SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                        if (initialHSStatus == NOT_HANDSHAKING || initialHSStatus == FINISHED) {
                            completeHandshake(Result(true, this, null))
                            break@needIO
                        }

                        val packetBufferSize = sslEngine.session.packetBufferSize
                        if (receivedPacketBuf.remaining() < packetBufferSize) {
                            break@needIO
                        }
                    }

                    SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                        resizeAppBuffer()
                        // retry the operation.
                    }

                    SSLEngineResult.Status.CLOSED -> {
                        log.info { "handshake failure. SSLEngine will close inbound. id: ${tcpConnection.id}" }
                        closeInbound()
                        break@needIO
                    }

                    else -> {
                        val ex =
                            SecureNetException("handshake exception. id: ${tcpConnection.id}, status: ${result.status}")
                        completeHandshake(Result(false, this, ex))
                        throw ex
                    }
                }
            }
        }
    }

    protected fun completeHandshake(result: Result<SecureEngine>) {
        if (initialHSComplete.compareAndSet(false, true)) {
            handshakeResult.accept(result)
        }
    }

    protected fun doHandshakeResponse() = try {
        outer@ while (initialHSStatus === SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            var result: SSLEngineResult
            var packetBuffer = newBuffer(sslEngine.session.packetBufferSize)

            wrap@ while (true) {
                result = sslEngine.wrap(hsBuffer, packetBuffer)
                initialHSStatus = result.handshakeStatus
                log.debug {
                    "handshake response. id: ${tcpConnection.id}, " +
                            "init: $initialHSStatus, ret: ${result.status}, complete: $initialHSComplete"
                }
                when (result.status) {
                    SSLEngineResult.Status.OK -> {
                        packetBuffer.flip()
                        log.debug { "handshake response. id: ${tcpConnection.id}, remaining: ${packetBuffer.remaining()}" }
                        when (initialHSStatus) {
                            NEED_TASK -> {
                                initialHSStatus = doTasks()
                                if (packetBuffer.hasRemaining()) {
                                    tcpConnection.write(packetBuffer)
                                }
                            }
                            FINISHED -> {
                                if (packetBuffer.hasRemaining()) {
                                    tcpConnection.write(packetBuffer) {
                                        completeHandshake(Result(it.isSuccess, this@AbstractSecureEngine, it.throwable))
                                    }
                                } else {
                                    completeHandshake(Result(true, this, null))
                                }
                            }
                            else -> {
                                if (packetBuffer.hasRemaining()) {
                                    tcpConnection.write(packetBuffer)
                                }
                            }
                        }
                        break@wrap
                    }

                    SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                        val b = newBuffer(packetBuffer.position() + sslEngine.session.packetBufferSize)
                        packetBuffer.flip()
                        b.put(packetBuffer)
                        packetBuffer = b
                    }

                    SSLEngineResult.Status.CLOSED -> {
                        log.info { "handshake failure. SSLEngine will close inbound. id: ${tcpConnection.id}" }
                        packetBuffer.flip()
                        if (packetBuffer.hasRemaining()) {
                            tcpConnection.write(packetBuffer)
                        }
                        closeOutbound()
                        break@outer
                    }

                    // BUFFER_UNDERFLOW
                    else -> {
                        val ex = SecureNetException(
                            "handshake exception. id: ${tcpConnection.id}, " +
                                    "status: ${result.status}"
                                                   )
                        completeHandshake(Result(false, this, ex))
                        throw ex
                    }
                }
            }
        }
    } catch (ex: IOException) {
        completeHandshake(Result(false, this, ex))
        throw ex
    }

    protected fun resizeAppBuffer() {
        val applicationBufferSize = sslEngine.session.applicationBufferSize
        val buf = newBuffer(receivedAppBuf.position() + applicationBufferSize)
        receivedAppBuf.flip()
        buf.put(receivedAppBuf)
        receivedAppBuf = buf
    }

    protected fun merge(now: ByteBuffer) {
        if (!now.hasRemaining()) {
            return
        }

        receivedPacketBuf = if (receivedPacketBuf.hasRemaining()) {
            log.debug {
                "read data. merge buffer. id: ${tcpConnection.id}, " +
                        "recPktBuf: ${receivedPacketBuf.remaining()}, " +
                        "now: ${now.remaining()}"
            }
            val ret = newBuffer(receivedPacketBuf.remaining() + now.remaining())
            ret.put(receivedPacketBuf).put(now).flip()
            ret
        } else {
            now
        }
    }

    protected fun getCurrentReceivedAppBuf(): ByteBuffer {
        receivedAppBuf.flip()
        log.debug {
            "read data. get app buf. id: ${tcpConnection.id}, " +
                    "pos: ${receivedAppBuf.position()}, " +
                    "limit: ${receivedAppBuf.limit()}"
        }
        return if (receivedAppBuf.hasRemaining()) {
            val buf = newBuffer(receivedAppBuf.remaining())
            buf.put(receivedAppBuf).flip()
            receivedAppBuf = newBuffer(sslEngine.session.applicationBufferSize)
            log.debug {
                "unwrap. id: ${tcpConnection.id}, remaining: ${buf.remaining()}"
            }
            buf
        } else {
            emptyBuf
        }
    }

    /**
     * Do all the outstanding handshake tasks in the current Thread.
     *
     * @return The result of handshake
     */
    protected fun doTasks(): SSLEngineResult.HandshakeStatus {
        // We could run this in a separate thread, but do in the current for now.
        while (true) {
            val task = sslEngine.delegatedTask
            if (task != null) {
                task.run()
            } else {
                break
            }
        }
        return sslEngine.handshakeStatus
    }


    fun close() {
        if (closed.compareAndSet(false, true)) {
            closeOutbound()
        }
    }

    protected fun closeInbound() {
        try {
            sslEngine.closeInbound()
        } catch (e: SSLException) {
            log.warn(e) { "close inbound exception" }
        } finally {
            tcpConnection.close()
        }
    }

    protected fun closeOutbound() {
        sslEngine.closeOutbound()
        tcpConnection.close()
    }

    protected abstract fun unwrap(input: ByteBuffer): SSLEngineResult

    protected abstract fun wrap(src: ByteBuffer, dst: ByteBuffer): SSLEngineResult

    protected abstract fun newBuffer(size: Int): ByteBuffer

    protected fun unwrap(): SSLEngineResult {
        val packetBufferSize = sslEngine.session.packetBufferSize
        //split net buffer when the net buffer remaining great than the net size
        val buf = splitBuffer(packetBufferSize)
        log.debug {
            "read data. id: ${tcpConnection.id}, " +
                    "buf: ${buf.remaining()}, " +
                    "packet: $packetBufferSize, " +
                    "appBuf: ${receivedAppBuf.remaining()}"
        }

        if (!receivedAppBuf.hasRemaining()) {
            resizeAppBuffer()
        }
        return unwrap(buf)
    }

    protected fun splitBuffer(netSize: Int): ByteBuffer {
        val buf = receivedPacketBuf.duplicate()
        return if (buf.remaining() <= netSize) {
            buf
        } else {
            val splitBuf = newBuffer(netSize)
            val data = ByteArray(netSize)
            buf.get(data)
            splitBuf.put(data).flip()
            splitBuf
        }
    }
}