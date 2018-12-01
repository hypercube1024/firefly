package com.fireflysource.common.pool

import com.fireflysource.common.concurrent.ExecutorServiceUtils.shutdownAndAwaitTermination
import com.fireflysource.common.func.Callback
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author Pengtao Qiu
 */
class TestAsyncBoundObjectPool {

    data class TestObject(var id: Int, var closed: Boolean = false)

    @Test
    fun test() = runBlocking {
        val idGenerator = AtomicInteger()
        val threadPool = Executors.newCachedThreadPool()

        val factory = Pool.ObjectFactory<TestObject> { pool ->
            val future = CompletableFuture<PooledObject<TestObject>>()
            val testObj = TestObject(idGenerator.getAndIncrement())
            val pooledObject = PooledObject(testObj, pool) { obj ->
                println("leaked: " + obj.getObject())
            }
            threadPool.submit {
                Thread.sleep(100) // mock the object creating consumption.
                future.complete(pooledObject)
            }
            future
        }
        val validator = Pool.Validator<TestObject> { pooledObject ->
            !pooledObject.getObject().closed
        }
        val dispose = Pool.Dispose<TestObject> { pooledObject ->
            pooledObject.getObject().closed = true
        }

        val maxSize = 4
        val pool = AsyncBoundObjectPool(
            maxSize, 60,
            factory, validator, dispose, 60, 60,
            Callback {
                println("no leak")
            })

        // get 20 test objects
        val list = List(20) {
            pool.asyncGet()
        }

        list.forEachIndexed { i, future ->
            println("The future is done. $i, ${future.isDone}, ${future.get()}")
            future.get().use { pooledObject ->
                assertFalse(pooledObject.getObject().closed)
                assertFalse(pooledObject.isReleased)
                assertTrue(pooledObject.getObject().id in 0..(maxSize - 1))
                println("test success. $i, $pooledObject")
            }

        }
        assertEquals(maxSize, pool.createdObjectCount)

        list.forEachIndexed { i, future ->
            println("complete test obj. $i, ${future.get()}")
            assertTrue(future.isDone)
        }

        pool.stop()
        shutdownAndAwaitTermination(threadPool, 5, TimeUnit.SECONDS)
        Unit
    }
}