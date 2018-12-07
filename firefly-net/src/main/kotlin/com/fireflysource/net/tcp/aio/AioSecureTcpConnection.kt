package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.Result
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AbstractTcpConnection.Companion.startReadingException
import com.fireflysource.net.tcp.secure.SecureEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
class AioSecureTcpConnection(
    private val tcpConnection: TcpConnection,
    private val secureEngine: SecureEngine,
    private val messageThread: CoroutineDispatcher
                            ) : TcpConnection by tcpConnection {

    companion object {
        private val log = SystemLogger.create(AioSecureTcpConnection::class.java)
    }

    private val decryptedInChannel: Channel<ByteBuffer> = Channel(Channel.UNLIMITED)
    private val encryptedOutChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    private var handshakeFinishedResult: Consumer<Result<String>> = Consumer {}
    private var receivedMessageConsumer: Consumer<ByteBuffer> = Consumer { buf ->
        decryptedInChannel.offer(buf)
    }

    init {
        launchWritingEncryptedMessageJob()
    }

    private fun launchWritingEncryptedMessageJob() = launchGlobally(messageThread) {
        try {
            secureEngine.beginHandshake().await()
            handshakeFinishedResult.accept(Result(true, applicationProtocol, null))
        } catch (e: Exception) {
            log.error(e) { "The TLS handshake failure." }
            tcpConnection.close()
            secureEngine.close()
            handshakeFinishedResult.accept(Result(false, "", e))
            throw e
        }

        while (!isShutdownOutput) {
            writeEncryptedMessage(encryptedOutChannel.receive())
        }
    }

    private fun writeEncryptedMessage(message: Message) {
        when (message) {
            is Buffer -> {
                val encryptedBuffers = secureEngine.encode(message.buffer)
                tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size) {
                    message.result.accept(Result(it.isSuccess, it.value.toInt(), it.throwable))
                }
            }
            is Buffers -> {
                val lastIndex = message.offset + message.length - 1
                val encryptedBuffers =
                    (message.offset..lastIndex).map { i -> secureEngine.encode(message.buffers[i]) }.flatten()
                tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size, message.result)
            }
            is BufferList -> {
                val lastIndex = message.offset + message.length - 1
                val encryptedBuffers =
                    (message.offset..lastIndex).map { i -> secureEngine.encode(message.bufferList[i]) }.flatten()
                tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size, message.result)
            }
            is Shutdown -> {
                tcpConnection.close(message.result)
            }
        }
    }

    override fun startReading(): TcpConnection {
        if (!tcpConnection.isStartReading) {
            tcpConnection.startReading()
            launchGlobally(messageThread) {
                val input = tcpConnection.inputChannel
                recvLoop@ while (true) {
                    val buf = input.receive()

                    readBufLoop@ while (buf.hasRemaining()) {
                        val decryptedBuf = secureEngine.decode(buf)
                        if (decryptedBuf.hasRemaining()) {
                            receivedMessageConsumer.accept(decryptedBuf)
                        }
                    }
                }
            }
        }
        return this
    }

    override fun onRead(messageConsumer: Consumer<ByteBuffer>): TcpConnection {
        if (!tcpConnection.isStartReading) {
            receivedMessageConsumer = messageConsumer
        } else {
            throw startReadingException
        }
        return this
    }

    override fun getInputChannel(): Channel<ByteBuffer> {
        return decryptedInChannel
    }

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        encryptedOutChannel.offer(Buffer(byteBuffer, result))
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
                      ): TcpConnection {
        encryptedOutChannel.offer(Buffers(byteBuffers, offset, length, result))
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>,
        offset: Int,
        length: Int,
        result: Consumer<Result<Long>>
                      ): TcpConnection {
        encryptedOutChannel.offer(BufferList(byteBufferList, offset, length, result))
        return this
    }

    override fun close(result: Consumer<Result<Void>>): TcpConnection {
        encryptedOutChannel.offer(Shutdown(result))
        return this
    }

    override fun isSecureConnection(): Boolean = true

    override fun isClientMode(): Boolean {
        return secureEngine.isClientMode
    }

    override fun isHandshakeFinished(): Boolean = secureEngine.isHandshakeFinished

    override fun onHandshakeFinished(result: Consumer<Result<String>>): TcpConnection {
        handshakeFinishedResult = result
        return this
    }

    override fun getSupportedApplicationProtocols(): List<String> {
        return secureEngine.supportedApplicationProtocols
    }

    override fun getApplicationProtocol(): String {
        return secureEngine.applicationProtocol
    }
}