package com.fireflysource.common.pool

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author Pengtao Qiu
 */
class TestAsyncBoundObjectPool {

    data class TestObject(var id: Int, var closed: Boolean = false)

    @Test
    @DisplayName("should get the pooled object successfully")
    fun test() = runBlocking {
        val id = AtomicInteger()
        val max = 4
        val destroyedChannel = Channel<PooledObject<TestObject>>(Channel.UNLIMITED)
        val pool = createAsyncPool(id, max, 60, 60, destroyedChannel)

        assertNotNull(pool.leakDetector)
        assertEquals(0, pool.size())
        assertTrue(pool.isEmpty)

        // get 20 test objects
        (1..20).asFlow().map { pool.takePooledObject() }
            .collect { pooledObject ->
                assertFalse(pooledObject.getObject().closed)
                assertFalse(pooledObject.isReleased)
                assertTrue(pooledObject.getObject().id in 0..max)
                println("1. get pooled object success. object: $pooledObject")

                pooledObject.closeFuture().await()
                assertTrue(pooledObject.isReleased)
            }

        assertEquals(max, pool.size())
        assertEquals(max, pool.createdObjectCount)
        println("current size: ${pool.size()}")

        repeat(20) {
            pool.takePooledObject().use { pooledObject ->
                assertFalse(pooledObject.getObject().closed)
                assertFalse(pooledObject.isReleased)
                assertTrue(pooledObject.getObject().id in 0..max)
                println("2. get pooled object success. object: $pooledObject")
            }
        }

        pool.stop()
    }

    @Test
    @DisplayName("should detect the pooled object is leak.")
    fun testPooledObjectLeak() = runBlocking {
        val id = AtomicInteger()
        val max = 10
        val destroyedChannel = Channel<PooledObject<TestObject>>(Channel.UNLIMITED)
        val pool = createAsyncPool(id, max, 1, 60, destroyedChannel)
        val pooledObject = pool.takePooledObject()
        val destroyedObject = destroyedChannel.receive()
        pool.leakDetector.clear(pooledObject)
        assertTrue(destroyedObject.getObject().closed)
        assertTrue(pooledObject.getObject().closed)
    }

    @Test
    fun testTimeout(): Unit = runBlocking {
        val id = AtomicInteger()
        val max = 4
        val destroyedChannel = Channel<PooledObject<TestObject>>(Channel.UNLIMITED)
        val pool = createAsyncPool(id, max, 60, 1, destroyedChannel)
        val array = Array(4) { pool.poll() }

        (1..8).map { pool.poll() }.forEach {
            try {
                it.await()
            } catch (e: Exception) {
                println(e.message)
                assertTrue(e is TimeoutException)
            }
        }

        array.forEach { it.await().closeFuture().await() }
        pool.takePooledObject().use { assertTrue(it.getObject().id in 0..max) }
        Unit
    }

    private fun createAsyncPool(
        id: AtomicInteger,
        max: Int,
        leakTime: Long,
        timeout: Long,
        destroyedChannel: Channel<PooledObject<TestObject>>
    ) = asyncPool<TestObject> {

        maxSize = max
        this.timeout = timeout
        leakDetectorInterval = leakTime
        releaseTimeout = leakTime

        objectFactory { pool ->
            delay(100)
            PooledObject(TestObject(id.getAndIncrement()), pool) { obj ->
                println("leaked: " + obj.getObject())
            }
        }

        validator { pooledObject ->
            !pooledObject.getObject().closed
        }

        dispose { pooledObject ->
            pooledObject.getObject().closed = true
            destroyedChannel.offer(pooledObject)
        }

        noLeakCallback {
            println("no leak")
        }

    }
}