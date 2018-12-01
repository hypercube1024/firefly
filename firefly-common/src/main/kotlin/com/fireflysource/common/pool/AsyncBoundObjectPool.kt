package com.fireflysource.common.pool

import com.fireflysource.common.concurrent.Atomics
import com.fireflysource.common.coroutine.asyncWithAttr
import com.fireflysource.common.coroutine.launchWithAttr
import com.fireflysource.common.exception.UnsupportedOperationException
import com.fireflysource.common.func.Callback
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.track.FixedTimeLeakDetector

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CompletableFuture
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
                             ) : AbstractLifeCycle(), Pool<T> {

    init {
        start()
    }

    private val createdCount = AtomicInteger(0)
    private val mutex = Mutex()
    private val channel = Channel<PooledObject<T>>(maxSize)
    private val leakDetector =
        FixedTimeLeakDetector<PooledObject<T>>(leakDetectorInterval, releaseTimeout, noLeakCallback)

    private class ArrivedMaxPoolSize(msg: String) : RuntimeException(msg)

    override fun asyncGet(): CompletableFuture<PooledObject<T>> = asyncWithAttr {
        try {
            createNewIfLessThanMaxSize()
        } catch (e: ArrivedMaxPoolSize) {
            getPooledObject()
        }
    }.asCompletableFuture()

    private suspend fun createNewIfLessThanMaxSize(): PooledObject<T> = mutex.withLock {
        if (createdCount.get() < maxSize) {
            val obj = objectFactory.createNew(this).await()
            createdCount.incrementAndGet()
            obj
        } else {
            throw ArrivedMaxPoolSize("Arrived the max pool size")
        }
    }

    private suspend fun getPooledObject(): PooledObject<T> {
        val pooledObject = channel.receive()
        return if (isValid(pooledObject)) {
            initGettingFromThePool(pooledObject)
            pooledObject
        } else {
            destroy(pooledObject)
            val newPooledObject = createNewIfLessThanMaxSize()
            initGettingFromThePool(pooledObject)
            newPooledObject
        }
    }

    private fun initGettingFromThePool(pooledObject: PooledObject<T>) {
        registerLeakTrack(pooledObject)
        pooledObject.released.set(false)
    }

    private fun registerLeakTrack(pooledObject: PooledObject<T>) {
        leakDetector.register(pooledObject) {
            createdCount.decrementAndGet()
            pooledObject.leakCallback.accept(pooledObject)
        }
    }

    private suspend fun destroy(pooledObject: PooledObject<T>) = mutex.withLock {
        Atomics.getAndDecrement(createdCount, 0)
        dispose.destroy(pooledObject)
    }

    private suspend fun asyncRelease(pooledObject: PooledObject<T>) {
        if (pooledObject.released.compareAndSet(false, true)) {
            leakDetector.clear(pooledObject)
            val success = channel.offer(pooledObject)
            if (!success) {
                destroy(pooledObject)
            }
        }
    }


    override fun get(): PooledObject<T> = asyncGet().get(timeout, TimeUnit.SECONDS)

    override fun release(pooledObject: PooledObject<T>) {
        launchWithAttr { asyncRelease(pooledObject) }
    }

    override fun isValid(pooledObject: PooledObject<T>): Boolean {
        return validator.isValid(pooledObject)
    }

    override fun size(): Int {
        throw UnsupportedOperationException("Can not get the channel size")
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException("Can not get the channel size")
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