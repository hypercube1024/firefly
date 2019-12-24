package com.fireflysource.common.pool

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.func.Callback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.future.asCompletableFuture

/**
 * @author Pengtao Qiu
 */
interface AsyncPool<T> : Pool<T> {

    suspend fun takePooledObject(): PooledObject<T>

    suspend fun putPooledObject(pooledObject: PooledObject<T>)

    fun getCoroutineDispatcher(): CoroutineDispatcher

}

class AsyncPoolBuilder<T> {

    var maxSize: Int = 16
    var timeout: Long = 30
    var leakDetectorInterval: Long = 60
    var releaseTimeout: Long = 60

    private lateinit var objectFactory: Pool.ObjectFactory<T>
    private lateinit var validator: Pool.Validator<T>
    private lateinit var dispose: Pool.Dispose<T>
    private lateinit var noLeakCallback: Callback


    fun objectFactory(createPooledObject: suspend (pool: AsyncPool<T>) -> PooledObject<T>) {
        objectFactory = Pool.ObjectFactory { pool ->
            require(pool is AsyncPool<T>)
            asyncGlobally(pool.getCoroutineDispatcher()) { createPooledObject.invoke(pool) }.asCompletableFuture()
        }
    }

    fun validator(isValid: (pooledObject: PooledObject<T>) -> Boolean) {
        validator = Pool.Validator { isValid.invoke(it) }
    }

    fun dispose(destroy: (pooledObject: PooledObject<T>) -> Unit) {
        dispose = Pool.Dispose { destroy.invoke(it) }
    }

    fun noLeakCallback(call: () -> Unit) {
        noLeakCallback = Callback { call.invoke() }
    }

    fun build(): AsyncPool<T> {
        Assert.isTrue(maxSize > 0, "The max size must be greater than 0")
        Assert.isTrue(timeout > 0, "The timeout must be greater than 0")
        Assert.isTrue(leakDetectorInterval > 0, "The leak detector interval must be greater than 0")
        Assert.isTrue(releaseTimeout > 0, "The release timeout must be greater than 0")

        return AsyncBoundObjectPool(
            maxSize,
            timeout,
            objectFactory,
            validator,
            dispose,
            leakDetectorInterval,
            releaseTimeout,
            noLeakCallback
        )
    }
}

fun <T> asyncPool(init: AsyncPoolBuilder<T>.() -> Unit): AsyncPool<T> {
    val pool = AsyncPoolBuilder<T>()
    pool.init()
    return pool.build()
}