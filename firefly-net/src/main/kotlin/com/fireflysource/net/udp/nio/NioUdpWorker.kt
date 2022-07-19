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
        while (true) {
            val count = selector.select()
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
                    when {
                        selectedKey.isReadable -> {

                        }
                        selectedKey.isWritable -> {

                        }
                    }
                    Unit
                }
                if (result.isFailure) {
                    log.error { "process nio selected key failure. $result" }
                }
                iterator.remove()
            }
        }
    }

}