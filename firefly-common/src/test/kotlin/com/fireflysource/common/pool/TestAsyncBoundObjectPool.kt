package com.fireflysource.common.pool

import kotlinx.coroutines.delay
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

        val pool = asyncPool<TestObject> {

            maxSize = max
            timeout = 60
            leakDetectorInterval = 10
            releaseTimeout = 60

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
            }

            noLeakCallback {
                println("no leak")
            }

        }

        assertNotNull(pool.leakDetector)
        assertEquals(0, pool.size())
        assertTrue(pool.isEmpty)

        // get 20 test objects
        val list = List(20) {
            pool.poll()
        }

        list.forEachIndexed { i, future ->
            //            println("The future is done. $i, ${future.isDone}, ${future.get()}")
            future.get().use { pooledObject ->
                assertFalse(pooledObject.getObject().closed)
                assertFalse(pooledObject.isReleased)
                assertTrue(pooledObject.getObject().id in 0 until max)
                println("test success. $i, $pooledObject")
            }
        }
        assertEquals(max, pool.createdObjectCount)

        list.forEachIndexed { i, future ->
            println("complete test obj. $i, ${future.get()}")
            assertTrue(future.isDone)
        }

        pool.poll().await().use { pooledObject ->
            assertFalse(pooledObject.getObject().closed)
            assertFalse(pooledObject.isReleased)
            assertTrue(pooledObject.getObject().id in 0 until max)
            println("test sync success. $pooledObject")
        }

        pool.stop()
    }
}