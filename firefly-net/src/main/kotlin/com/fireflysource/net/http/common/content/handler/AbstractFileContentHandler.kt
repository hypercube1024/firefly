package com.fireflysource.net.http.common.content.handler

import com.fireflysource.common.coroutine.asVoidFuture
import com.fireflysource.common.io.closeAsync
import com.fireflysource.common.io.openFileChannelAsync
import com.fireflysource.common.io.writeAwait
import com.fireflysource.common.sys.SystemLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer
import java.nio.file.OpenOption
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

abstract class AbstractFileContentHandler<T>(val path: Path, vararg options: OpenOption) : HttpContentHandler<T> {

    companion object {
        private val log = SystemLogger.create(AbstractFileContentHandler::class.java)
    }

    private val inputChannel: Channel<WriteFileMessage> = Channel(Channel.UNLIMITED)
    private val scope: CoroutineScope = CoroutineScope(CoroutineName("Firefly-file-content-handler"))
    private val writeJob: Job = scope.launch {
        val fileChannel = openFileChannelAsync(path, *options).await()
        var pos = 0L

        suspend fun closeFileChannel() {
            try {
                fileChannel.closeAsync().join()
            } catch (e: Exception) {
                log.error(e) { "close file channel exception." }
            }
        }

        suspend fun write(): Boolean {
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
                write()
            } catch (e: Exception) {
                log.error(e) { "read file exception." }
                closeFileChannel()
                true
            }
            if (closed) break@writeMessageLoop
        }
    }


    override fun accept(buffer: ByteBuffer, t: T) {
        inputChannel.trySend(WriteFileRequest(buffer))
    }

    override fun closeAsync(): CompletableFuture<Void> =
        scope.launch { closeAwait() }.asVoidFuture().thenAccept { scope.cancel() }

    override fun close() {
        inputChannel.trySend(EndWriteFile)
    }

    private suspend fun closeAwait() {
        close()
        writeJob.join()
    }

}

sealed interface WriteFileMessage
@JvmInline
value class WriteFileRequest(val buffer: ByteBuffer) : WriteFileMessage
object EndWriteFile : WriteFileMessage
