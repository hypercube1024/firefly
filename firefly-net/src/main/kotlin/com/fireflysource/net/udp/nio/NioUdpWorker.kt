package com.fireflysource.net.udp.nio

import com.fireflysource.common.concurrent.ExecutorServiceUtils.shutdownAndAwaitTermination
import com.fireflysource.common.coroutine.CoroutineDispatchers.newSingleThreadExecutor
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.copy
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.udp.UdpConnection
import com.fireflysource.net.udp.buffer.NioWorkerMessage
import com.fireflysource.net.udp.buffer.RegisterRead
import com.fireflysource.net.udp.exception.UdpAttachmentTypeException
import org.jctools.queues.MpscLinkedQueue
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class NioUdpWorker(
    id: Int
) : AbstractLifeCycle(), Runnable {

    companion object {
        private val log = SystemLogger.create(NioUdpWorker::class.java)
        private val seconds = TimeUnit.SECONDS
    }

    private val executor = newSingleThreadExecutor("firefly-nio-udp-worker-thread-$id")
    private val selector = Selector.open()
    private val workerMessageQueue = MpscLinkedQueue<NioWorkerMessage>()
    private val inputBuffer = BufferUtils.allocateDirect(8 * 1024)

    override fun init() {
        executor.execute(this)
    }

    override fun destroy() {
        val closeResult = runCatching { selector.close() }
        log.info { "Nio UDP worker selector close result: $closeResult" }
        shutdownAndAwaitTermination(executor, 5, seconds)
    }

    override fun run() {
        while (true) {
            val count = selectKeys()
            handleNioUdpWorkerMessages()
            if (count == 0)
                continue

            val iterator = selector.selectedKeys().iterator()
            while (iterator.hasNext()) {
                val selectedKey = iterator.next()
                iterator.remove()

                val result = runCatching {
                    val udpConnection = selectedKey.attachment()
                    if (udpConnection !is AbstractNioUdpConnection) {
                        throw UdpAttachmentTypeException("attachment type exception. ${udpConnection::class.java.name}")
                    }
                    val datagramChannel = udpConnection.datagramChannel

                    fun readComplete(): ReadResult {
                        val pos = inputBuffer.flipToFill()
                        val result = runCatching { datagramChannel.read(inputBuffer) }
                        inputBuffer.flipToFlush(pos)

                        return if (result.isSuccess) {
                            val length = result.getOrDefault(0)
                            when {
                                length > 0 -> {
                                    udpConnection.sendReadCompleteMessage(inputBuffer.copy())
                                    ReadResult.CONTINUE_READ
                                }

                                length == 0 -> {
                                    ReadResult.CONTINUE_READ
                                }

                                else -> {
                                    ReadResult.REMOTE_CLOSE
                                }
                            }
                        } else {
                            ReadResult.REMOTE_CLOSE
                        }
                    }

                    fun writeComplete(): WriteResult {
                        return WriteResult.CONTINUE_WRITE
                    }

                    fun cancelSelectedKey() {
                        selectedKey.cancel()
                        selector.selectNow()
                        udpConnection.sendCancelSelectionKeyMessage()
                    }
                    if (selectedKey.isValid) {
                        if (selectedKey.isReadable) {
                            when (readComplete()) {
                                ReadResult.CONTINUE_READ -> TODO()
                                ReadResult.REMOTE_CLOSE -> {
                                    val unregisterReadResult = runCatching {
                                        selectedKey.interestOps(selectedKey.interestOps() and SelectionKey.OP_READ.inv())
                                    }
                                    if (unregisterReadResult.isSuccess) {
                                        udpConnection.sendUnregisterReadMessage()
                                    } else {
                                        val e = unregisterReadResult.exceptionOrNull()
                                        if (e != null && e is IllegalArgumentException) {
                                            cancelSelectedKey()
                                        }
                                    }
                                }

                                ReadResult.SUSPEND_READ -> TODO()
                                ReadResult.READ_EXCEPTION -> TODO()
                            }
                        }
                        if (selectedKey.isWritable) {
                            when (writeComplete()) {
                                WriteResult.REMOTE_CLOSE -> {
                                }

                                WriteResult.SUSPEND_WRITE -> TODO()
                                WriteResult.CONTINUE_WRITE -> TODO()
                                WriteResult.WRITE_EXCEPTION -> TODO()
                            }
                        }
                    } else {
                        udpConnection.sendInvalidSelectionKeyMessage()
                    }
                }
                if (result.isFailure) {
                    log.error { "handle nio selected key failure. $result" }
                }
            }
        }
    }

    fun registerRead(udpConnection: UdpConnection): CompletableFuture<SelectionKey> {
        val future = CompletableFuture<SelectionKey>()
        sendMessage(RegisterRead(udpConnection, future))
        return future
    }

    private fun selectKeys(): Int {
        return selector.select()
    }

    private fun sendMessage(message: NioWorkerMessage) {
        if (workerMessageQueue.offer(message)) {
            selector.wakeup()
        }
    }

    private fun handleNioUdpWorkerMessages() {
        while (true) {
            val message = workerMessageQueue.poll() ?: break
            when (message) {
                is RegisterRead -> {
                    if (message.udpConnection is AbstractNioUdpConnection) {
                        val udpConnection = message.udpConnection
                        val datagramChannel = message.udpConnection.datagramChannel
                        val key = datagramChannel.register(
                            selector,
                            SelectionKey.OP_READ,
                            udpConnection
                        )
                        message.future.complete(key)
                    }
                }
            }
        }
    }

}