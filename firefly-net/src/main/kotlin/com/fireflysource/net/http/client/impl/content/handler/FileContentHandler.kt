package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.common.coroutine.CoroutineDispatchers
import com.fireflysource.common.coroutine.CoroutineDispatchers.singleThread
import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.io.writeAwait
import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.OpenOption
import java.nio.file.Path

class FileContentHandler(val path: Path, vararg options: OpenOption) : HttpClientContentHandler, Closeable {

    private val fileChannel = AsynchronousFileChannel.open(path, setOf(*options), CoroutineDispatchers.ioBlockingPool)
    private val inputChannel: Channel<ByteBuffer> = Channel(Channel.UNLIMITED)
    private val writeJob: Job

    companion object {
        private val closeFlag = ByteBuffer.allocate(0)
    }

    init {
        writeJob = launchGlobally(singleThread) {
            var pos = 0L
            msgLoop@ while (true) {
                val buf = inputChannel.receive()
                if (buf == closeFlag) {
                    break@msgLoop
                }

                writeLoop@ while (buf.hasRemaining()) {
                    val len = fileChannel.writeAwait(buf, pos)
                    if (len <= 0) {
                        break@msgLoop
                    }
                    pos += len
                }
            }

            @Suppress("BlockingMethodInNonBlockingContext")
            fileChannel.close()
        }
    }

    override fun accept(buffer: ByteBuffer, response: HttpClientResponse) {
        inputChannel.offer(buffer)
    }

    override fun close() {
        inputChannel.offer(closeFlag)
    }

    suspend fun closeAwait() {
        close()
        writeJob.join()
    }
}