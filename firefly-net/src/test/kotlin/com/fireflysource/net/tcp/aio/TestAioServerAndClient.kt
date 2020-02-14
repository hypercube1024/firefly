package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.CoroutineDispatchers.defaultPoolSize
import com.fireflysource.common.coroutine.event
import com.fireflysource.common.coroutine.eventAsync
import com.fireflysource.common.lifecycle.AbstractLifeCycle.stopAll
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.net.tcp.onAcceptAsync
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis


/**
 * @author Pengtao Qiu
 */
class TestAioServerAndClient {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                arguments("single", true),
                arguments("array", true),
                arguments("list", true),
                arguments("single", false),
                arguments("array", false),
                arguments("list", false)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should send and receive messages successfully.")
    fun test(bufType: String, enableSecure: Boolean) = runBlocking {
        val host = "localhost"
        val port = Random.nextInt(10000, 20000)

        val connectionCount = defaultPoolSize
        val maxMessageCountPerOneConnection = 100
        val expectMessageCount = maxMessageCountPerOneConnection * connectionCount

        val messageCount = AtomicInteger()
        val tcpConfig = TcpConfig(30, enableSecure)

        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            println("accept connection. ${connection.id}")
            connection.beginHandshake().await()
            recvLoop@ while (true) {
                val buf = try {
                    connection.read().await()
                } catch (e: Exception) {
                    break@recvLoop
                }

                readBufLoop@ while (buf.hasRemaining()) {
                    val num = buf.int
                    val newBuf = ByteBuffer.allocate(4)
                    newBuf.putInt(num).flip()
                    connection.write(newBuf, discard())
                }
            }
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val time = measureTimeMillis {
            val jobs = (1..connectionCount).map {
                event {
                    val connection = client.connect(host, port).await()
                    println("create connection. ${connection.id}")
                    connection.beginHandshake().await()

                    val readingJob = connection.coroutineScope.launch {
                        recvLoop@ while (true) {
                            val buf = try {
                                connection.read().await()
                            } catch (e: Exception) {
                                break@recvLoop
                            }

                            readBufLoop@ while (buf.hasRemaining()) {
                                val num = buf.int
                                messageCount.incrementAndGet()

                                if (num == maxMessageCountPerOneConnection) {
                                    connection.closeFuture().await()
                                    break@recvLoop
                                }
                            }
                        }
                    }

                    when (bufType) {
                        "single" -> {
                            (1..maxMessageCountPerOneConnection).forEach { i ->
                                val buf = ByteBuffer.allocate(4)
                                buf.putInt(i).flip()
                                connection.write(buf, discard())
                            }
                        }
                        "array" -> {
                            val bufArray = Array<ByteBuffer>(maxMessageCountPerOneConnection) { index ->
                                val buf = ByteBuffer.allocate(4)
                                buf.putInt(index + 1).flip()
                                buf
                            }
                            connection.write(bufArray, 0, bufArray.size, discard())
                        }
                        "list" -> {
                            val bufList = List<ByteBuffer>(maxMessageCountPerOneConnection) { index ->
                                val buf = ByteBuffer.allocate(4)
                                buf.putInt(index + 1).flip()
                                buf
                            }
                            connection.write(bufList, 0, bufList.size, discard())
                        }
                    }

                    readingJob.join()

                    assertTrue(connection.readBytes > 0)
                    assertTrue(connection.writtenBytes > 0)
                    assertTrue(connection.lastActiveTime > 0L)
                    assertTrue(connection.lastReadTime > 0L)
                    assertTrue(connection.lastWrittenTime > 0L)
                }
            }

            jobs.forEach { it.join() }

            assertEquals(expectMessageCount, messageCount.get())
        }

        val throughput = expectMessageCount / (time / 1000.00)
        println("success. $time ms, ${throughput.roundToLong()} qps")
        println("connection: ${connectionCount}, messageCount: $expectMessageCount")

        val stopTime = measureTimeMillis {
            stopAll()
        }
        println("stop success. $stopTime")
    }

    @Test
    @DisplayName("should close when the connection is timeout.")
    fun testTimeout() = runBlocking {
        val host = "localhost"
        val port = 4001

        AioTcpServer(TcpConfig(1)).onAcceptAsync { conn ->
            try {
                conn.read().await()
                println("Server reads success.")
            } catch (e: Exception) {
                println("Server reads failure. ${e.javaClass.name}")
            }
        }.listen(host, port)

        val client = AioTcpClient(TcpConfig(1))
        val connection = client.connect(host, port).await()
        assertEquals(port, connection.remoteAddress.port)

        val success = try {
            connection.read().await()
            true
        } catch (e: Exception) {
            assertTrue(e is InterruptedByTimeoutException)
            false
        }
        assertFalse(success)
        assertTrue(connection.isShutdownInput)
        assertTrue(connection.duration > 0)

        val stopTime = measureTimeMillis {
            stopAll()
        }
        println("stop success. $stopTime")
    }

    @Test
    @DisplayName("should close connection successfully.")
    fun testClose(): Unit = runBlocking {
        val host = "localhost"
        val port = 4002

        AioTcpServer().onAcceptAsync { conn ->
            try {
                conn.read().await()
                println("Server reads success.")
            } catch (e: Exception) {
                println("Server reads failure. ${e.javaClass.name}")
            }
        }.listen(host, port)

        val client = AioTcpClient()
        val connection = client.connect(host, port).await()

        val success = eventAsync {
            try {
                connection.read().await()
                println("Client reads success.")
                true
            } catch (e: Exception) {
                println("Client reads failure. ${e.javaClass.name}")
                false
            }
        }

        connection.closeFuture().await()
        println("close success")
        assertTrue(connection.isShutdownOutput)
        assertTrue(connection.isShutdownInput)
        assertTrue(connection.isClosed)
        assertFalse(success.await())
    }
}