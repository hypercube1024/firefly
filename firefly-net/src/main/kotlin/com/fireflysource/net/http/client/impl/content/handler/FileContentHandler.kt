package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.common.coroutine.CoroutineDispatchers.singleThread
import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.io.asyncClose
import com.fireflysource.common.io.asyncOpenFileChannel
import com.fireflysource.common.io.writeAwait
import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.file.OpenOption
import java.nio.file.Path

class FileContentHandler(val path: Path, vararg options: OpenOption) : HttpClientContentHandler, Closeable {

    private val inputChannel: Channel<WriteFileMessage> = Channel(Channel.UNLIMITED)
    private val writeJob: Job

    init {
        writeJob = launchGlobally(singleThread) {
            val fileChannel = asyncOpenFileChannel(path, *options).await()
            var pos = 0L
            writeMessageLoop@ while (true) {
                when (val writeFileMessage = inputChannel.receive()) {
                    is WriteFileRequest -> {
                        val buf = writeFileMessage.buffer
                        flushDataLoop@ while (buf.hasRemaining()) {
                            val len = fileChannel.writeAwait(buf, pos)
                            if (len <= 0) {
                                fileChannel.asyncClose()
                                break@writeMessageLoop
                            }
                            pos += len
                        }
                    }
                    is EndWriteFile -> {
                        fileChannel.asyncClose()
                        break@writeMessageLoop
                    }
                }
            }
        }
    }

    override fun accept(buffer: ByteBuffer, response: HttpClientResponse) {
        inputChannel.offer(WriteFileRequest(buffer))
    }

    override fun close() {
        inputChannel.offer(EndWriteFile)
    }

    suspend fun closeAwait() {
        close()
        writeJob.join()
    }
}

sealed class WriteFileMessage
data class WriteFileRequest(val buffer: ByteBuffer) : WriteFileMessage()
object EndWriteFile : WriteFileMessage()