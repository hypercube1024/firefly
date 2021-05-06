package com.fireflysource.common.pool

import com.fireflysource.common.coroutine.CoroutineDispatchers.scheduler
import com.fireflysource.common.coroutine.event
import com.fireflysource.common.coroutine.pollAll
import com.fireflysource.common.func.Callback
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.common.track.FixedTimeLeakDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import java.util.*
import java.util.concurrent.*

/**
 * @author Pengtao Qiu
 */
class AsyncBoundObjectPool<T>(
    private val maxSize: Int,
    private val timeout: Long,
    private val objectFactory: Pool.ObjectFactory<T>,
    private val validator: Pool.Validator<T>,
    private val dispose: Pool.Dispose<T>,
    leakDetectorInterval: Long,
    releaseTimeout: Long,
    noLeakCallback: Callback
) : AbstractLifeCycle(), AsyncPool<T> {

    companion object {
        private val log = SystemLogger.create(AsyncBoundObjectPool::class.java)
    }

    private var createdCount = 0
    private var size = 0
    private val pool: LinkedList<PooledObject<T>> = LinkedList()
    private val waitQueue: LinkedList<PollObject<T>> = LinkedList()
    private val poolMessageChannel: Channel<PoolMessage<T>> = Channel(Channel.UNLIMITED)
    private val leakDetector = FixedTimeLeakDetector<PooledObject<T>>(
        scheduler,
        leakDetectorInterval, leakDetectorInterval, releaseTimeout, TimeUnit.SECONDS,
        noLeakCallback
    )
    private val handlePoolMessageJob = handlePoolMessage()

    init {
        start()
    }

    override suspend fun takePooledObject(): PooledObject<T> = poll().await()

    override fun poll(): CompletableFuture<PooledObject<T>> {
        val future = CompletableFuture<PooledObject<T>>()
        val timeoutJob: ScheduledFuture<*> = scheduler.schedule({
            if (!future.isDone) {
                future.completeExceptionally(TimeoutException("Take pooled object timeout"))
            }
        }, timeout, TimeUnit.SECONDS)
        poolMessageChannel.trySend(PollObject(future, timeoutJob)).isSuccess
        return future
    }

    private fun handlePoolMessage(): Job {
        val job = event {
            while (true) {
                when (val message = poolMessageChannel.receive()) {
                    is PollObject<T> -> handlePollObjectMessage(message)
                    is ReleaseObject<T> -> handleReleaseObjectMessage(message)
                }
            }
        }
        job.invokeOnCompletion { cause ->
            if (cause != null) {
                log.info { "The pool message job completion. cause: ${cause.message}" }
            }

            clearMessage()
        }
        return job
    }

    private fun clearMessage() {
        poolMessageChannel.pollAll {
            when (it) {
                is PollObject<T> -> it.future.completeExceptionally(IllegalStateException("The pool has closed."))
                is ReleaseObject<T> -> it.future.completeExceptionally(IllegalStateException("The pool has closed."))
            }
        }
        while (true) {
            val m: PollObject<T>? = waitQueue.poll()
            if (m != null) {
                m.future.completeExceptionally(IllegalStateException("The pool has closed."))
            } else break
        }
    }

    private suspend fun handlePollObjectMessage(message: PollObject<T>) {
        try {
            val pooledObject = createNew() ?: getFromPool()
            if (pooledObject != null) {
                initPooledObject(pooledObject)
                message.future.complete(pooledObject)
                message.timeoutJob.cancel(true)
            } else waitQueue.offer(message)
        } catch (e: Exception) {
            log.error(e) { "Handle poll object message exception." }
            message.future.completeExceptionally(e)
            message.timeoutJob.cancel(true)
        }
    }

    private fun initPooledObject(pooledObject: PooledObject<T>) {
        pooledObject.released.set(false)
        leakDetector.register(pooledObject) {
            try {
                pooledObject.leakCallback.accept(it)
            } catch (e: Exception) {
                log.error(e) { "The pooled object has leaked. object: $it ." }
            } finally {
                destroyPooledObject(it)
            }
        }
    }

    private suspend fun createNew(): PooledObject<T>? = if (createdCount < maxSize) {
        val pooledObject = objectFactory.createNew(this).await()
        createdCount++
        log.debug { "create a new object. $pooledObject" }
        pooledObject
    } else null

    private suspend fun getFromPool(): PooledObject<T>? {
        val oldPooledObject: PooledObject<T>? = pool.poll()
        return if (oldPooledObject != null) {
            size--
            if (isValid(oldPooledObject)) {
                log.debug { "get an old object. $oldPooledObject" }
                oldPooledObject
            } else {
                destroyPooledObject(oldPooledObject)
                val newPooledObject = createNew()
                requireNotNull(newPooledObject)
                newPooledObject
            }
        } else null
    }

    private fun destroyPooledObject(pooledObject: PooledObject<T>) {
        try {
            dispose.destroy(pooledObject)
        } catch (e: Exception) {
            log.error(e) { "destroy pooled object exception." }
        } finally {
            log.debug { "destroy the object: $pooledObject ." }
            createdCount--
            if (createdCount < 0) {
                log.error { "The created object count must not be less than 0" }
                createdCount = 0
            }
        }
    }

    private suspend fun handleReleaseObjectMessage(message: ReleaseObject<T>) {
        val (pooledObject, future) = message
        if (pooledObject.released.compareAndSet(false, true)) {
            leakDetector.clear(pooledObject)
            pool.offer(pooledObject)
            size++
            Result.done(future)
            log.debug { "release pooled object: $pooledObject, pool size: ${size()}." }
            handleWaitingMessage()
        }
    }

    private suspend fun handleWaitingMessage() {
        while (true) {
            val pollObjectMessage: PollObject<T>? = waitQueue.poll()
            if (pollObjectMessage != null) {
                if (!pollObjectMessage.future.isDone) {
                    handlePollObjectMessage(pollObjectMessage)
                    break
                } else {
                    log.debug { "Discard the polling message when it is done." }
                }
            } else break
        }
    }

    override fun release(pooledObject: PooledObject<T>): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        poolMessageChannel.trySend(ReleaseObject(pooledObject, future)).isSuccess
        return future
    }

    override suspend fun putPooledObject(pooledObject: PooledObject<T>) {
        release(pooledObject).await()
    }

    override fun isValid(pooledObject: PooledObject<T>): Boolean {
        return try {
            validator.isValid(pooledObject)
        } catch (e: Exception) {
            log.error(e) { "Valid pooled object exception" }
            false
        }
    }

    override fun size(): Int = size

    override fun isEmpty(): Boolean {
        return size() == 0
    }

    override fun getLeakDetector(): FixedTimeLeakDetector<PooledObject<T>> = leakDetector

    override fun getCreatedObjectCount(): Int = createdCount

    override fun init() {
    }

    override fun destroy() {
        leakDetector.stop()
        handlePoolMessageJob.cancel(CancellationException("Cancel object pool message job exception."))
        clearMessage()
        pool.forEach { destroyPooledObject(it) }
        pool.clear()
    }

}

sealed class PoolMessage<T>
class PollObject<T>(val future: CompletableFuture<PooledObject<T>>, val timeoutJob: ScheduledFuture<*>) :
    PoolMessage<T>()

data class ReleaseObject<T>(val pooledObject: PooledObject<T>, val future: CompletableFuture<Void>) : PoolMessage<T>()
