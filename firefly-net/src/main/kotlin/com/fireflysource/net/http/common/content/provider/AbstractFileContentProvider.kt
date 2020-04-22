package com.fireflysource.net.http.common.content.provider

import com.fireflysource.common.coroutine.event
import com.fireflysource.common.coroutine.pollAll
import com.fireflysource.common.exception.UnsupportedOperationException
import com.fireflysource.common.io.InputChannel
import com.fireflysource.common.io.closeJob
import com.fireflysource.common.io.openFileChannelAsync
import com.fireflysource.common.io.readAwait
import com.fireflysource.common.sys.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractFileContentProvider(
    val path: Path,
    val options: Set<OpenOption>,
    var position: Long,
    val length: Long
) : InputChannel {

    private val readChannel: Channel<ReadFileMessage> = Channel(Channel.UNLIMITED)
    private val readJob: Job
    private val closed = AtomicBoolean(false)
    private val lastPosition = position + length

    constructor(path: Path, vararg options: OpenOption) : this(path, options.toSet(), 0, Files.size(path))

    init {
        readJob = event {
            val fileChannel = openFileChannelAsync(path, options).await()

            readMessageLoop@ while (true) {
                when (val readFileMessage = readChannel.receive()) {
                    is ReadFileRequest -> {
                        val (buf, future) = readFileMessage

                        suspend fun endRead() {
                            fileChannel.closeJob().join()
                            closed.set(true)
                            future.complete(-1)
                        }

                        if (position >= lastPosition) {
                            endRead()
                            break@readMessageLoop
                        }
                        try {
                            val len = fileChannel.readAwait(buf, position)
                            if (len < 0) {
                                endRead()
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
                        fileChannel.closeJob().join()
                        closed.set(true)
                        break@readMessageLoop
                    }
                }
            }

            readChannel.pollAll { }
        }
    }

    fun length(): Long = length

    override fun isOpen(): Boolean = !closed.get()

    fun toByteBuffer(): ByteBuffer {
        throw UnsupportedOperationException("The file content does not support this method")
    }

    override fun closeFuture(): CompletableFuture<Void> =
        event { closeAwait() }.asCompletableFuture().thenCompose { Result.DONE }

    override fun close() {
        if (isOpen) readChannel.offer(EndReadFile)
    }

    private suspend fun closeAwait() {
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