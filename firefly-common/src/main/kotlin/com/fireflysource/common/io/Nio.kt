@file:Suppress("BlockingMethodInNonBlockingContext", "KDocUnresolvedReference")

package com.fireflysource.common.io

import com.fireflysource.common.coroutine.CoroutineDispatchers.ioBlockingThreadPool
import com.fireflysource.common.coroutine.blocking
import com.fireflysource.common.coroutine.blockingAsync
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.future.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit

/**
 * Performs [AsynchronousFileChannel.lock] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
suspend fun AsynchronousFileChannel.lockAwait() = suspendCancellableCoroutine<FileLock> { cont ->
    lock(cont, asyncIOHandler())
    closeOnCancel(cont)
}

/**
 * Performs [AsynchronousFileChannel.lock] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
suspend fun AsynchronousFileChannel.lockAwait(
    position: Long,
    size: Long,
    shared: Boolean
) = suspendCancellableCoroutine<FileLock> { cont ->
    lock(position, size, shared, cont, asyncIOHandler())
    closeOnCancel(cont)
}

suspend fun <T : Closeable?, R> T.useAwait(block: suspend (T) -> R): R {
    try {
        return block(this)
    } catch (e: Throwable) {
        throw e
    } finally {
        withContext(NonCancellable) {
            this@useAwait?.closeJob()?.join()
        }
    }
}

suspend fun <T : AsyncCloseable?, R> T.useAwait(block: suspend (T) -> R): R {
    try {
        return block(this)
    } catch (e: Throwable) {
        throw e
    } finally {
        withContext(NonCancellable) {
            this@useAwait?.closeFuture()?.await()
        }
    }
}

/**
 * Close in the I/O blocking coroutine dispatcher
 */
fun Closeable.closeJob() = blocking {
    close()
}

fun openFileChannelAsync(file: Path, vararg options: OpenOption) = blockingAsync {
    AsynchronousFileChannel.open(file, setOf(*options), ioBlockingThreadPool)
}

fun openFileChannelAsync(file: Path, options: Set<OpenOption>) = blockingAsync {
    AsynchronousFileChannel.open(file, options, ioBlockingThreadPool)
}

fun readAllLinesAsync(file: Path, charset: Charset = StandardCharsets.UTF_8) = blockingAsync {
    Files.readAllLines(file, charset)
}

fun readAllBytesAsync(file: Path) = blockingAsync {
    Files.readAllBytes(file)
}

fun deleteIfExistsAsync(file: Path) = blockingAsync {
    Files.deleteIfExists(file)
}

fun existsAsync(file: Path, vararg options: LinkOption) = blockingAsync {
    Files.exists(file, *options)
}

fun readAttributesAsync(file: Path, vararg options: LinkOption) = blockingAsync {
    Files.readAttributes(file, BasicFileAttributes::class.java, *options)
}

fun writeAsync(file: Path, iterable: Iterable<CharSequence>, vararg options: OpenOption) = blockingAsync {
    Files.write(file, iterable, *options)
}

/**
 * Performs [AsynchronousFileChannel.read] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
suspend fun AsynchronousFileChannel.readAwait(
    buf: ByteBuffer,
    position: Long
) = suspendCancellableCoroutine<Int> { cont ->
    read(buf, position, cont, asyncIOHandler())
    closeOnCancel(cont)
}

/**
 * Performs [AsynchronousFileChannel.write] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
suspend fun AsynchronousFileChannel.writeAwait(
    buf: ByteBuffer,
    position: Long
) = suspendCancellableCoroutine<Int> { cont ->
    write(buf, position, cont, asyncIOHandler())
    closeOnCancel(cont)
}

/**
 * Performs [AsynchronousServerSocketChannel.accept] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
suspend fun AsynchronousServerSocketChannel.acceptAwait() =
    suspendCancellableCoroutine<AsynchronousSocketChannel> { cont ->
        accept(cont, asyncIOHandler())
        closeOnCancel(cont)
    }

/**
 * Performs [AsynchronousSocketChannel.connect] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
suspend fun AsynchronousSocketChannel.connectAwait(
    socketAddress: SocketAddress
) = suspendCancellableCoroutine<Unit> { cont ->
    connect(socketAddress, cont, AsyncVoidIOHandler)
    closeOnCancel(cont)
}

/**
 * Performs [AsynchronousSocketChannel.read] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
suspend fun AsynchronousSocketChannel.readAwait(
    buf: ByteBuffer,
    timeout: Long = 0L,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) = suspendCancellableCoroutine<Int> { cont ->
    read(buf, timeout, timeUnit, cont, asyncIOHandler())
    closeOnCancel(cont)
}

suspend fun AsynchronousSocketChannel.readAwait(
    buffers: Array<ByteBuffer>,
    offset: Int,
    length: Int,
    timeout: Long = 0L,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) = suspendCancellableCoroutine<Long> { cont ->
    read(buffers, offset, length, timeout, timeUnit, cont, asyncIOHandler())
    closeOnCancel(cont)
}

/**
 * Performs [AsynchronousSocketChannel.write] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
suspend fun AsynchronousSocketChannel.writeAwait(
    buf: ByteBuffer,
    timeout: Long = 0L,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) = suspendCancellableCoroutine<Int> { cont ->
    write(buf, timeout, timeUnit, cont, asyncIOHandler())
    closeOnCancel(cont)
}

suspend fun AsynchronousSocketChannel.writeAwait(
    buffers: Array<ByteBuffer>,
    offset: Int,
    length: Int,
    timeout: Long = 0L,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) = suspendCancellableCoroutine<Long> { cont ->
    write(buffers, offset, length, timeout, timeUnit, cont, asyncIOHandler())
    closeOnCancel(cont)
}

// ---------------- private details ----------------

private fun Channel.closeOnCancel(cont: CancellableContinuation<*>) {
    cont.invokeOnCancellation {
        try {
            close()
        } catch (ex: Throwable) {
            // Specification says that it is Ok to call it any time, but reality is different,
            // so we have just to ignore exception
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> asyncIOHandler(): CompletionHandler<T, CancellableContinuation<T>> =
    AsyncIOHandlerAny as CompletionHandler<T, CancellableContinuation<T>>

private object AsyncIOHandlerAny : CompletionHandler<Any, CancellableContinuation<Any>> {
    override fun completed(result: Any, cont: CancellableContinuation<Any>) {
        cont.resumeWith(Result.success(result))
    }

    override fun failed(ex: Throwable, cont: CancellableContinuation<Any>) {
        // just return if already cancelled and got an expected exception for that case
        if (ex is AsynchronousCloseException && cont.isCancelled) {
            return
        }
        cont.resumeWith(Result.failure(ex))
    }
}

private object AsyncVoidIOHandler : CompletionHandler<Void?, CancellableContinuation<Unit>> {
    override fun completed(result: Void?, cont: CancellableContinuation<Unit>) {
        cont.resumeWith(Result.success(Unit))
    }

    override fun failed(ex: Throwable, cont: CancellableContinuation<Unit>) {
        // just return if already cancelled and got an expected exception for that case
        if (ex is AsynchronousCloseException && cont.isCancelled) {
            return
        }
        cont.resumeWith(Result.failure(ex))
    }
}
