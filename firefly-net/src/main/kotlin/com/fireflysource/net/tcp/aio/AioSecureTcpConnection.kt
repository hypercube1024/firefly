package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.io.asyncClose
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
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
    private val encryptedOutChannel: Channel<OutputMessage> = Channel(Channel.UNLIMITED)
    private var handshakeCompleteResult: Consumer<Result<String>> = Consumer {}
    private var receivedMessageConsumer: Consumer<ByteBuffer> = Consumer { buf ->
        decryptedInChannel.offer(buf)
    }

    init {
        launchWritingEncryptedMessageJob()
    }

    private fun launchWritingEncryptedMessageJob() = launchGlobally(messageThread) {
        try {
            secureEngine.beginHandshake().await()
            handshakeCompleteResult.accept(Result(true, applicationProtocol, null))
        } catch (e: Exception) {
            log.error(e) { "The TLS handshake failure." }
            tcpConnection.asyncClose().join()
            secureEngine.asyncClose().join()
            handshakeCompleteResult.accept(Result(false, "", e))
            throw e
        }

        while (!isShutdownOutput) {
            writeEncryptedMessage(encryptedOutChannel.receive())
        }
    }

    private fun writeEncryptedMessage(outputMessage: OutputMessage) {
        when (outputMessage) {
            is Buffer -> {
                val encryptedBuffers = secureEngine.encode(outputMessage.buffer)
                tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size) {
                    outputMessage.result.accept(
                        Result(
                            it.isSuccess,
                            it.value.toInt(),
                            it.throwable
                        )
                    )
                }
            }
            is Buffers -> {
                val lastIndex = outputMessage.offset + outputMessage.length - 1
                val encryptedBuffers =
                    (outputMessage.offset..lastIndex).map { i -> secureEngine.encode(outputMessage.buffers[i]) }.flatten()
                tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size, outputMessage.result)
            }
            is BufferList -> {
                val lastIndex = outputMessage.offset + outputMessage.length - 1
                val encryptedBuffers =
                    (outputMessage.offset..lastIndex).map { i -> secureEngine.encode(outputMessage.bufferList[i]) }.flatten()
                tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size, outputMessage.result)
            }
            is Shutdown -> {
                tcpConnection.close(outputMessage.result)
            }
        }
    }

    override fun startReading(): TcpConnection {
        if (!tcpConnection.isReading) {
            tcpConnection.startReading()
            launchGlobally(messageThread) {
                val input = tcpConnection.inputChannel
                recvLoop@ while (tcpConnection.isReading) {
                    val buf = input.receive()

                    readBufLoop@ while (buf.hasRemaining()) {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        val decryptedBuf = secureEngine.decode(buf)
                        if (decryptedBuf.hasRemaining()) {
                            receivedMessageConsumer.accept(decryptedBuf)
                        }
                    }
                }
            }
            log.info { "stop receiving encrypted messages. id: $id" }
        }
        return this
    }

    override fun onRead(messageConsumer: Consumer<ByteBuffer>): TcpConnection {
        if (!tcpConnection.isReading) {
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

    override fun isHandshakeComplete(): Boolean = secureEngine.isHandshakeComplete

    override fun onHandshakeComplete(result: Consumer<Result<String>>): TcpConnection {
        handshakeCompleteResult = result
        return this
    }

    override fun getSupportedApplicationProtocols(): List<String> {
        return secureEngine.supportedApplicationProtocols
    }

    override fun getApplicationProtocol(): String {
        return secureEngine.applicationProtocol
    }
}