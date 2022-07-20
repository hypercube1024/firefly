package com.fireflysource.net.udp.nio

import com.fireflysource.common.concurrent.ExecutorServiceUtils.shutdownAndAwaitTermination
import com.fireflysource.common.coroutine.CoroutineDispatchers.newSingleThreadExecutor
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.udp.exception.UdpAttachmentTypeException
import java.nio.channels.Selector
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
                        when {
                            selectedKey.isReadable -> {
                                val length = udpConnection.readComplete()
                                if (length < 0) {
                                    selectedKey.cancel()
                                    hasCancelledKeys = true
                                }
                            }
                            selectedKey.isWritable -> udpConnection.writeComplete()
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

}