package com.fireflysource.net.http.common.content.handler

import com.fireflysource.common.coroutine.event
import com.fireflysource.common.io.AsyncCloseable
import com.fireflysource.common.io.closeAsync
import com.fireflysource.common.io.openFileChannelAsync
import com.fireflysource.common.io.writeAwait
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import java.nio.ByteBuffer
import java.nio.file.OpenOption
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer

abstract class AbstractFileContentHandler<T>(val path: Path, vararg options: OpenOption) :
    AsyncCloseable, BiConsumer<ByteBuffer, T> {

    companion object {
        private val log = SystemLogger.create(AbstractFileContentHandler::class.java)
    }

    private val inputChannel: Channel<WriteFileMessage> = Channel(Channel.UNLIMITED)
    private val writeJob: Job

    init {
        writeJob = event {
            val fileChannel = openFileChannelAsync(path, *options).await()
            var pos = 0L

            suspend fun closeFileChannel() {
                try {
                    fileChannel.closeAsync().join()
                } catch (e: Exception) {
                    log.error(e) { "close file channel exception." }
                }
            }

            suspend fun read(): Boolean {
                var closed = false
                when (val writeFileMessage = inputChannel.receive()) {
                    is WriteFileRequest -> {
                        val buf = writeFileMessage.buffer
                        flushDataLoop@ while (buf.hasRemaining()) {
                            val len = fileChannel.writeAwait(buf, pos)
                            if (len < 0) {
                                closeFileChannel()
                                closed = true
                            }
                            pos += len
                        }
                    }
                    is EndWriteFile -> {
                        closeFileChannel()
                        closed = true
                    }
                }
                return closed
            }

            writeMessageLoop@ while (true) {
                val closed = try {
                    read()
                } catch (e: Exception) {
                    log.error(e) { "read file exception." }
                    closeFileChannel()
                    true
                }
                if (closed) break@writeMessageLoop
            }
        }
    }


    override fun accept(buffer: ByteBuffer, t: T) {
        inputChannel.offer(WriteFileRequest(buffer))
    }

    override fun closeFuture(): CompletableFuture<Void> =
        event { closeAwait() }.asCompletableFuture().thenCompose { Result.DONE }

    override fun close() {
        inputChannel.offer(EndWriteFile)
    }

    private suspend fun closeAwait() {
        close()
        writeJob.join()
    }

}

sealed class WriteFileMessage
class WriteFileRequest(val buffer: ByteBuffer) : WriteFileMessage()
object EndWriteFile : WriteFileMessage()
