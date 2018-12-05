package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.launchWithAttr
import com.fireflysource.net.tcp.Result
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.secure.SecureEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer
import java.util.concurrent.Executors
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
        private val secureCodecThread: CoroutineDispatcher by lazy {
            Executors.newSingleThreadExecutor { Thread(it, "firefly-tcp-secure-codec-thread") }.asCoroutineDispatcher()
        }
        private val handshakeHasBegunException = HandshakeException("handshake has begun exception")
        private val handshakeHasNotBegunException = HandshakeException("handshake has not begun exception")
        private val handshakeHasNotFinishedException = HandshakeException("handshake has not finished")
    }

    private val decryptedInputChannel: Channel<ByteBuffer> = Channel(Channel.UNLIMITED)
    private val isBeganHandshake = AtomicBoolean(false)
    private var receivedMessageConsumer: Consumer<ByteBuffer> = Consumer { buf ->
        decryptedInputChannel.offer(buf)
    }

    override fun beginHandshake(result: Consumer<Result<Void>>) {
        if (isBeganHandshake.compareAndSet(false, true)) {
            secureEngine.beginHandshake(result)
        } else {
            result.accept(Result<Void>(false, null, handshakeHasBegunException))
        }
    }

    override fun startReading(): TcpConnection {
        if (isBeganHandshake.get()) {
            tcpConnection.startReading()
            launchWithAttr(secureCodecThread) {
                val input = tcpConnection.inputChannel
                recvLoop@ while (true) {
                    val buf = input.receive()

                    readBufLoop@ while (buf.hasRemaining()) {
                        val decryptedBuf = secureEngine.decode(buf)
                        if (decryptedBuf != null && decryptedBuf.hasRemaining()) {
                            receivedMessageConsumer.accept(decryptedBuf)
                        }
                    }
                }
            }
        } else {
            throw handshakeHasNotBegunException
        }
        return this
    }

    override fun onRead(messageConsumer: Consumer<ByteBuffer>): TcpConnection {
        receivedMessageConsumer = messageConsumer
        return this
    }

    override fun getInputChannel(): Channel<ByteBuffer> {
        return decryptedInputChannel
    }

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        if (isHandshakeFinished) {
            val buf = secureEngine.encode(byteBuffer)
            tcpConnection.write(buf, 0, buf.size) {
                result.accept(Result(it.isSuccess, it.value.toInt(), it.throwable))
            }
        } else {
            result.accept(Result(false, -1, handshakeHasNotFinishedException))
        }
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
                      ): TcpConnection {
        if (isHandshakeFinished) {
            val lastIndex = offset + length - 1
            val encryptedBuffers = (offset..lastIndex).map { i -> secureEngine.encode(byteBuffers[i]) }.flatten()
            tcpConnection.write(encryptedBuffers, offset, length, result)
        } else {
            result.accept(Result(false, -1, handshakeHasNotFinishedException))
        }
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
                      ): TcpConnection {
        if (isHandshakeFinished) {
            val lastIndex = offset + length - 1
            val encryptedBuffers = (offset..lastIndex).map { i -> secureEngine.encode(byteBufferList[i]) }.flatten()
            tcpConnection.write(encryptedBuffers, offset, length, result)
        } else {
            result.accept(Result(false, -1, handshakeHasNotFinishedException))
        }
        return this
    }


    override fun isSecureConnection(): Boolean = true

    override fun isClientMode(): Boolean {
        return secureEngine.isClientMode
    }

    override fun isHandshakeFinished(): Boolean = secureEngine.isHandshakeFinished

    override fun getSupportedApplicationProtocols(): List<String> {
        return secureEngine.supportedApplicationProtocols
    }

    override fun getApplicationProtocol(): String {
        return secureEngine.applicationProtocol
    }
}

class HandshakeException(msg: String) : IllegalStateException(msg)