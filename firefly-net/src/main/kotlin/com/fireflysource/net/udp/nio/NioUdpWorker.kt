package com.fireflysource.net.udp.nio

import com.fireflysource.common.concurrent.ExecutorServiceUtils.shutdownAndAwaitTermination
import com.fireflysource.common.coroutine.CoroutineDispatchers.newSingleThreadExecutor
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.udp.UdpConnection
import com.fireflysource.net.udp.buffer.NioWorkerMessage
import com.fireflysource.net.udp.buffer.RegisterRead
import com.fireflysource.net.udp.exception.UdpAttachmentTypeException
import org.jctools.queues.MpscLinkedQueue
import java.nio.channels.DatagramChannel
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

    override fun init() {
        executor.execute(this)
    }

    override fun destroy() {
        val closeResult = runCatching {
            selector.close()
        }
        log.info { "Nio UDP worker selector close result: $closeResult" }
        shutdownAndAwaitTermination(executor, 5, seconds)
    }

    override fun run() {
        var hasCancelledKeys = false

        fun selectKeys(): Int {
            val count = if (hasCancelledKeys) {
                val selectedKeyNowCount = selector.selectNow()
                if (selectedKeyNowCount > 0) selectedKeyNowCount else selector.select()
            } else selector.select()
            hasCancelledKeys = false
            return count
        }

        while (true) {
            val count = selectKeys()
            handleNioUdpWorkerMessages()
            if (count == 0)
                continue

            val iterator = selector.selectedKeys().iterator()
            while (iterator.hasNext()) {
                val selectedKey = iterator.next()
                val result = runCatching {
                    val udpConnection = selectedKey.attachment()
                    if (udpConnection !is AbstractNioUdpConnection) {
                        throw UdpAttachmentTypeException("attachment type exception. ${udpConnection::class.java.name}")
                    }
                    if (selectedKey.isValid) {
                        var isReadCancel = false
                        var isWriteCancel = false
                        if (selectedKey.isReadable) {
                            when (udpConnection.readComplete()) {
                                ReadResult.REMOTE_CLOSE -> {
                                    isReadCancel = true
                                }
                                ReadResult.SUSPEND_READ -> TODO()
                                ReadResult.CONTINUE_READ -> TODO()
                            }
                        }
                        if (selectedKey.isWritable) {
                            when (udpConnection.writeComplete()) {
                                WriteResult.REMOTE_CLOSE -> TODO()
                                WriteResult.SUSPEND_WRITE -> TODO()
                                WriteResult.CONTINUE_WRITE -> TODO()
                            }
                        }
                    } else {
                        udpConnection.sendInvalidSelectionKeyMessage()
                    }
                    Unit
                }
                if (result.isFailure) {
                    log.error { "handle nio selected key failure. $result" }
                }
                iterator.remove()
            }
        }
    }

    fun registerRead(datagramChannel: DatagramChannel, udpConnection: UdpConnection): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        workerMessageQueue.offer(RegisterRead(datagramChannel, udpConnection, future))
        selector.wakeup()
        return future
    }

    private fun handleNioUdpWorkerMessages() {
        while (true) {
            val message = workerMessageQueue.poll() ?: break
            when(message) {
                is RegisterRead -> {
                    message.datagramChannel.register(selector, SelectionKey.OP_READ, message.udpConnection)
                    message.future.complete(null)
                }
            }
        }
    }

}