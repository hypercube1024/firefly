package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.common.coroutine.launchSingle
import com.fireflysource.common.io.closeAsync
import com.fireflysource.common.io.openFileChannelAsync
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
        writeJob = launchSingle {
            val fileChannel = openFileChannelAsync(path, *options).await()
            var pos = 0L
            writeMessageLoop@ while (true) {
                when (val writeFileMessage = inputChannel.receive()) {
                    is WriteFileRequest -> {
                        val buf = writeFileMessage.buffer
                        flushDataLoop@ while (buf.hasRemaining()) {
                            val len = fileChannel.writeAwait(buf, pos)
                            if (len <= 0) {
                                fileChannel.closeAsync()
                                break@writeMessageLoop
                            }
                            pos += len
                        }
                    }
                    is EndWriteFile -> {
                        fileChannel.closeAsync()
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