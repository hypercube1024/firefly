package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.coroutine.CoroutineDispatchers.singleThread
import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.exception.UnsupportedOperationException
import com.fireflysource.common.io.asyncClose
import com.fireflysource.common.io.asyncOpenFileChannel
import com.fireflysource.common.io.readAwait
import com.fireflysource.net.http.client.HttpClientContentProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class FileContentProvider(val path: Path, vararg options: OpenOption) : HttpClientContentProvider {

    private val readChannel: Channel<ReadFileMessage> = Channel(Channel.UNLIMITED)
    private val readJob: Job
    private val length = Files.size(path)
    private var position: Long = 0

    @Volatile
    private var closed: Boolean = false

    init {
        readJob = launchGlobally(singleThread) {
            val fileChannel = asyncOpenFileChannel(path, *options).await()

            readMessageLoop@ while (true) {
                when (val readFileMessage = readChannel.receive()) {
                    is ReadFileRequest -> {
                        val (buf, future) = readFileMessage
                        try {
                            val len = fileChannel.readAwait(buf, position)
                            if (len < 0) {
                                future.complete(len)
                                fileChannel.asyncClose()
                                closed = true
                                break@readMessageLoop
                            } else {
                                position += len
                                future.complete(len)
                            }
                        } catch (e: Exception) {
                            future.completeExceptionally(e)
                        }
                    }
                    is EndReadFile -> {
                        fileChannel.asyncClose().join()
                        closed = true
                        break@readMessageLoop
                    }
                }
            }
        }
    }

    override fun length(): Long = length

    override fun isOpen(): Boolean = !closed

    override fun toByteBuffer(): ByteBuffer {
        throw UnsupportedOperationException("The file content does not support this method")
    }

    override fun close() {
        readChannel.offer(EndReadFile)
    }

    suspend fun closeAwait() {
        close()
        readJob.join()
    }

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        readChannel.offer(ReadFileRequest(byteBuffer, future))
        return future
    }

}

sealed class ReadFileMessage
data class ReadFileRequest(val buffer: ByteBuffer, val future: CompletableFuture<Int>) : ReadFileMessage()
object EndReadFile : ReadFileMessage()