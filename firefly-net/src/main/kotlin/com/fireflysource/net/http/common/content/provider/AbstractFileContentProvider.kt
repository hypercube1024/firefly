package com.fireflysource.net.http.common.content.provider

import com.fireflysource.common.coroutine.asVoidFuture
import com.fireflysource.common.coroutine.clear
import com.fireflysource.common.exception.UnsupportedOperationException
import com.fireflysource.common.io.InputChannel
import com.fireflysource.common.io.closeAsync
import com.fireflysource.common.io.openFileChannelAsync
import com.fireflysource.common.io.readAwait
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
    val length: Long,
    val scope: CoroutineScope = CoroutineScope(CoroutineName("Firefly-file-content-provider"))
) : InputChannel {

    private val readChannel: Channel<ReadFileMessage> = Channel(Channel.UNLIMITED)
    private val readJob: Job
    private val closed = AtomicBoolean(false)
    private val lastPosition = position + length

    constructor(path: Path, vararg options: OpenOption) : this(path, options.toSet(), 0, Files.size(path))

    init {
        readJob = scope.launch {
            val fileChannel = openFileChannelAsync(path, options).await()

            readMessageLoop@ while (true) {
                when (val readFileMessage = readChannel.receive()) {
                    is ReadFileRequest -> {
                        val (buf, future) = readFileMessage

                        suspend fun endRead() {
                            fileChannel.closeAsync().join()
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
                        fileChannel.closeAsync().join()
                        closed.set(true)
                        break@readMessageLoop
                    }
                }
            }

            readChannel.clear()
        }
    }

    fun length(): Long = length

    override fun isOpen(): Boolean = !closed.get()

    fun toByteBuffer(): ByteBuffer {
        throw UnsupportedOperationException("The file content does not support this method")
    }

    override fun closeAsync(): CompletableFuture<Void> =
        scope.launch { closeAwait() }.asVoidFuture().thenAccept { scope.cancel() }

    override fun close() {
        if (isOpen) readChannel.trySend(EndReadFile)
    }

    private suspend fun closeAwait() {
        close()
        readJob.join()
    }

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        readChannel.trySend(ReadFileRequest(byteBuffer, future))
        return future
    }

}

sealed class ReadFileMessage
data class ReadFileRequest(val buffer: ByteBuffer, val future: CompletableFuture<Int>) : ReadFileMessage()
object EndReadFile : ReadFileMessage()