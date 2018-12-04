package com.fireflysource.common.pool

import com.fireflysource.common.concurrent.Atomics
import com.fireflysource.common.coroutine.asyncWithAttr
import com.fireflysource.common.coroutine.launchWithAttr
import com.fireflysource.common.func.Callback
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.CommonLogger
import com.fireflysource.common.track.FixedTimeLeakDetector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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
        private val log = CommonLogger.create(AsyncBoundObjectPool::class.java)
        private val objectPoolThread: CoroutineDispatcher by lazy {
            Executors.newSingleThreadExecutor { Thread(it, "firefly-object-pool-thread") }.asCoroutineDispatcher()
        }
    }

    init {
        start()
    }

    private val createdCount = AtomicInteger(0)
    private val size = AtomicInteger(0)
    private val mutex = Mutex()
    private val channel = Channel<PooledObject<T>>(maxSize)
    private val leakDetector =
        FixedTimeLeakDetector<PooledObject<T>>(leakDetectorInterval, releaseTimeout, noLeakCallback)

    private class ArrivedMaxPoolSize(msg: String) : RuntimeException(msg)

    override suspend fun asyncGet(): PooledObject<T> = getObjectDeferred().await()

    override fun get(): CompletableFuture<PooledObject<T>> = getObjectDeferred().asCompletableFuture()

    private fun getObjectDeferred(): Deferred<PooledObject<T>> = asyncWithAttr(objectPoolThread) {
        try {
            createNewIfLessThanMaxSize()
        } catch (e: ArrivedMaxPoolSize) {
            getFromPool()
        }
    }

    private suspend fun createNewIfLessThanMaxSize(): PooledObject<T> = mutex.withLock {
        if (createdCount.get() < maxSize) {
            val obj = objectFactory.createNew(this).await()
            createdCount.incrementAndGet()
            log.debug { "create a new object. $obj" }
            obj
        } else {
            throw ArrivedMaxPoolSize("Arrived the max pool size")
        }
    }

    private suspend fun getFromPool(): PooledObject<T> {
        val oldPooledObject = channel.receive()
        size.decrementAndGet()
        return if (isValid(oldPooledObject)) {
            initPooledObject(oldPooledObject)
            log.debug { "get an old object. $oldPooledObject" }
            oldPooledObject
        } else {
            destroy(oldPooledObject)
            val newPooledObject = createNewIfLessThanMaxSize()
            initPooledObject(oldPooledObject)
            newPooledObject
        }
    }

    private fun initPooledObject(pooledObject: PooledObject<T>) {
        leakDetector.register(pooledObject) {
            createdCount.decrementAndGet()
            pooledObject.leakCallback.accept(pooledObject)
        }
        pooledObject.released.set(false)
    }

    private suspend fun destroy(pooledObject: PooledObject<T>) = mutex.withLock {
        log.debug { "destroy the object. $pooledObject" }
        Atomics.getAndDecrement(createdCount, 0)
        dispose.destroy(pooledObject)
    }


    override fun take(): PooledObject<T> = get().get(timeout, TimeUnit.SECONDS)

    override fun release(pooledObject: PooledObject<T>) {
        launchWithAttr(objectPoolThread) {
            if (pooledObject.released.compareAndSet(false, true)) {
                leakDetector.clear(pooledObject)
                channel.send(pooledObject)
                size.incrementAndGet()
                log.debug { "release the object. $pooledObject, ${size()}" }
            }
        }
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
        leakDetector.stop()
        channel.close()
    }
}