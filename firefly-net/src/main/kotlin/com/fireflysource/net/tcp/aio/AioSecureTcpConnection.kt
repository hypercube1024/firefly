package com.fireflysource.net.tcp.aio

import com.fireflysource.common.sys.Result
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AbstractAioTcpConnection.Companion.startReadingException
import com.fireflysource.net.tcp.secure.SecureEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
class AioSecureTcpConnection(
    private val tcpConnection: TcpConnection,
    private val secureEngine: SecureEngine
) : TcpConnection by tcpConnection {

    private val decryptedInChannel: Channel<ByteBuffer> = Channel(Channel.UNLIMITED)
    private val encryptedOutChannel: Channel<OutputMessage> = Channel(Channel.UNLIMITED)
    private var receivedMessageConsumer: Consumer<ByteBuffer> = Consumer { buf ->
        decryptedInChannel.offer(buf)
    }

    init {
        tcpConnection.onClose { secureEngine.close() }
    }

    private fun launchDecryptionJob() = tcpConnection.coroutineScope.launch {
        val input = tcpConnection.inputChannel
        recvLoop@ while (tcpConnection.isReading) {
            val buf = input.receive()

            readBufLoop@ while (buf.hasRemaining()) {
                val decryptedBuf = secureEngine.decrypt(buf)
                if (decryptedBuf.hasRemaining()) {
                    receivedMessageConsumer.accept(decryptedBuf)
                }
            }
        }
    }

    private fun launchWritingEncryptedMessageJob() = tcpConnection.coroutineScope.launch {
        while (!isShutdownOutput) {
            writeEncryptedMessage(encryptedOutChannel.receive())
        }
    }

    private fun writeEncryptedMessage(outputMessage: OutputMessage) {
        when (outputMessage) {
            is Buffer -> encryptAndFlushBuffer(outputMessage)
            is Buffers -> encryptAndFlushBuffers(outputMessage)
            is BufferList -> encryptAndFlushBuffers(outputMessage)
            is Shutdown -> tcpConnection.close(outputMessage.result)
        }
    }

    private fun encryptAndFlushBuffers(buffers: Buffers) {
        val offset = buffers.getCurrentOffset()
        val lastIndex = buffers.getLastIndex()
        val bufferArray = buffers.getBuffers()
        val encryptedBuffers = (offset..lastIndex)
            .map { i -> secureEngine.encrypt(bufferArray[i]) }
            .flatten()
        tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size, buffers.getResult())
    }

    private fun encryptAndFlushBuffer(outputMessage: Buffer) {
        val encryptedBuffers = secureEngine.encrypt(outputMessage.buffer)
        tcpConnection.write(encryptedBuffers, 0, encryptedBuffers.size) {
            outputMessage.result.accept(Result(it.isSuccess, it.value.toInt(), it.throwable))
        }
    }

    override fun onRead(messageConsumer: Consumer<ByteBuffer>): TcpConnection {
        if (tcpConnection.isReading) {
            throw startReadingException
        }

        receivedMessageConsumer = messageConsumer
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
        secureEngine.beginHandshake()
            .thenAccept {
                result.accept(Result(true, applicationProtocol, null))
                launchWritingEncryptedMessageJob()
                launchDecryptionJob()
            }.exceptionally { e ->
                result.accept(Result(false, "", e))
                null
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