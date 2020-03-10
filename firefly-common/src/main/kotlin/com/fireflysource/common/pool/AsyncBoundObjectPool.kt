package com.fireflysource.common.pool

import com.fireflysource.common.concurrent.Atomics
import com.fireflysource.common.coroutine.CoroutineDispatchers.scheduler
import com.fireflysource.common.coroutine.event
import com.fireflysource.common.func.Callback
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.common.track.FixedTimeLeakDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.time.withTimeout
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

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

    private val createdCount = AtomicInteger(0)
    private val size = AtomicInteger(0)
    private val poolChannel = Channel<PooledObject<T>>(maxSize)
    private val pollTaskChannel = Channel<CompletableFuture<PooledObject<T>>>(Channel.UNLIMITED)
    private val releaseTaskChannel = Channel<ReleasePooledObjectMessage>(Channel.UNLIMITED)
    private val leakDetector = FixedTimeLeakDetector<PooledObject<T>>(
        scheduler,
        leakDetectorInterval, leakDetectorInterval, releaseTimeout, TimeUnit.SECONDS,
        noLeakCallback
    )
    private val pollObjectJob: Job
    private val releaseObjectJob: Job

    init {
        pollObjectJob = launchPollObjectJob()
        releaseObjectJob = launchReleaseObjectJob()
        start()
    }

    override suspend fun takePooledObject(): PooledObject<T> = poll().await()

    override fun poll(): CompletableFuture<PooledObject<T>> {
        val future = CompletableFuture<PooledObject<T>>()
        pollTaskChannel.offer(future)
        return future
    }

    private fun launchPollObjectJob(): Job = event {
        while (true) {
            val future = pollTaskChannel.receive()
            try {
                val pooledObject = createNew() ?: getFromPool()
                initPooledObject(pooledObject)
                future.complete(pooledObject)
            } catch (e: Exception) {
                log.error(e) { "poll object from pool exception." }
                future.completeExceptionally(e)
            }
        }
    }

    private fun initPooledObject(pooledObject: PooledObject<T>) {
        leakDetector.register(pooledObject, Consumer {
            try {
                pooledObject.leakCallback.accept(it)
            } catch (e: Exception) {
                log.error(e) { "The pooled object has leaked. object: $it ." }
            } finally {
                destroyPooledObject(it)
            }
        })
        pooledObject.released.set(false)
    }

    private suspend fun createNew(): PooledObject<T>? = if (createdCount.get() < maxSize) {
        val pooledObject = objectFactory.createNew(this).await()
        createdCount.incrementAndGet()
        log.debug { "create a new object. $pooledObject" }
        pooledObject
    } else null

    private suspend fun getFromPool(): PooledObject<T> {
        val oldPooledObject = withTimeout(Duration.ofSeconds(timeout)) { poolChannel.receive() }
        size.decrementAndGet()
        return if (isValid(oldPooledObject)) {
            log.debug { "get an old object. $oldPooledObject" }
            oldPooledObject
        } else {
            destroyPooledObject(oldPooledObject)
            val pooledObject = createNew()
            requireNotNull(pooledObject)
            pooledObject
        }
    }

    private fun destroyPooledObject(pooledObject: PooledObject<T>) {
        try {
            dispose.destroy(pooledObject)
        } catch (e: Exception) {
            log.error(e) { "destroy pooled object exception." }
        } finally {
            log.debug { "destroy the object: $pooledObject ." }
            Atomics.getAndDecrement(createdCount, 0)
        }
    }


    // release task
    private fun launchReleaseObjectJob(): Job = event {
        while (true) {
            val message = releaseTaskChannel.receive()
            val pooledObject = message.pooledObject
            val future = message.future
            try {
                if (pooledObject.released.compareAndSet(false, true)) {
                    leakDetector.clear(pooledObject)
                    withTimeout(Duration.ofSeconds(timeout)) { poolChannel.send(pooledObject) }
                    size.incrementAndGet()
                    future.complete(null)
                    log.debug { "release pooled object: $pooledObject, pool size: ${size()}." }
                }
            } catch (e: Exception) {
                log.error(e) { "release pooled object exception" }
                destroyPooledObject(pooledObject)
                future.completeExceptionally(e)
            }
        }
    }

    override fun release(pooledObject: PooledObject<T>): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        releaseTaskChannel.offer(ReleasePooledObjectMessage(pooledObject, future))
        return future
    }

    override suspend fun putPooledObject(pooledObject: PooledObject<T>) {
        release(pooledObject).await()
    }

    override fun isValid(pooledObject: PooledObject<T>): Boolean {
        return validator.isValid(pooledObject)
    }

    override fun size(): Int = size.get()

    override fun isEmpty(): Boolean {
        return size() == 0
    }

    override fun getLeakDetector(): FixedTimeLeakDetector<PooledObject<T>> = leakDetector

    override fun getCreatedObjectCount(): Int = createdCount.get()

    override fun init() {
    }

    override fun destroy() {
        while (true) {
            val o = poolChannel.poll()
            if (o == null) {
                break
            } else {
                dispose.destroy(o)
            }
        }

        leakDetector.stop()
        poolChannel.close()
        pollObjectJob.cancel(CancellationException("Cancel object pool polling job exception."))
        releaseObjectJob.cancel(CancellationException("Cancel object pool release job exception."))
    }

    inner class ReleasePooledObjectMessage(
        val pooledObject: PooledObject<T>,
        val future: CompletableFuture<Void>
    )
}