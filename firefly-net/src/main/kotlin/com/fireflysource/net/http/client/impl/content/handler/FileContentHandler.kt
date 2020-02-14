package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.common.coroutine.event
import com.fireflysource.common.io.closeAsync
import com.fireflysource.common.io.openFileChannelAsync
import com.fireflysource.common.io.writeAwait
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer
import java.nio.file.OpenOption
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class FileContentHandler(val path: Path, vararg options: OpenOption) : HttpClientContentHandler {

    private val inputChannel: Channel<WriteFileMessage> = Channel(Channel.UNLIMITED)
    private val writeJob: Job

    init {
        writeJob = event {
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
                        try {
                            fileChannel.closeAsync().join()
                            Result.done(writeFileMessage.future)
                        } catch (e: Exception) {
                            writeFileMessage.future.completeExceptionally(e)
                        }
                        break@writeMessageLoop
                    }
                }
            }
        }
    }

    override fun accept(buffer: ByteBuffer, response: HttpClientResponse) {
        inputChannel.offer(WriteFileRequest(buffer))
    }

    override fun closeFuture(): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        inputChannel.offer(EndWriteFile(future))
        return future
    }

    override fun close() {
        closeFuture()
    }
}

sealed class WriteFileMessage
class WriteFileRequest(val buffer: ByteBuffer) : WriteFileMessage()
class EndWriteFile(val future: CompletableFuture<Void>) : WriteFileMessage()