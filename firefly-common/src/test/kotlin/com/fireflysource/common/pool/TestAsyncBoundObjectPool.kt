package com.fireflysource.common.pool

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author Pengtao Qiu
 */
class TestAsyncBoundObjectPool {

    data class TestObject(var id: Int, var closed: Boolean = false)

    @Test
    fun test() = runBlocking {
        val id = AtomicInteger()
        val max = 4
        val destroyedChannel = Channel<PooledObject<TestObject>>(Channel.UNLIMITED)
        val pool = createAsyncPool(id, max, 60, destroyedChannel)

        assertNotNull(pool.leakDetector)
        assertEquals(0, pool.size())
        assertTrue(pool.isEmpty)

        // get 20 test objects
        (1..20).asFlow().map { pool.takePooledObject() }
            .collect { pooledObject ->
                assertFalse(pooledObject.getObject().closed)
                assertFalse(pooledObject.isReleased)
                assertTrue(pooledObject.getObject().id in 0..max)
                println("get pooled object success. object: $pooledObject")

                pooledObject.release().await()
                assertTrue(pooledObject.isReleased)
            }

        assertEquals(max, pool.size())
        assertEquals(max, pool.createdObjectCount)
        println("current size: ${pool.size()}")

        pool.takePooledObject().use { pooledObject ->
            assertFalse(pooledObject.getObject().closed)
            assertFalse(pooledObject.isReleased)
            assertTrue(pooledObject.getObject().id in 0..max)
            println("get pooled object success. object: $pooledObject")
        }

        pool.stop()
    }

    @Test
    fun testPooledObjectLeak() = runBlocking {
        val id = AtomicInteger()
        val max = 10
        val destroyedChannel = Channel<PooledObject<TestObject>>(Channel.UNLIMITED)
        val pool = createAsyncPool(id, max, 1, destroyedChannel)
        val pooledObject = pool.takePooledObject()
        val destroyedObject = destroyedChannel.receive()
        pool.leakDetector.clear(pooledObject)
        assertTrue(destroyedObject.getObject().closed)
        assertTrue(pooledObject.getObject().closed)
    }

    private fun createAsyncPool(
        id: AtomicInteger,
        max: Int,
        time: Long,
        destroyedChannel: Channel<PooledObject<TestObject>>
    ) = asyncPool<TestObject> {

        maxSize = max
        timeout = 60
        leakDetectorInterval = time
        releaseTimeout = time

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