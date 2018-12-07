package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.launchGlobal
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream
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
    fun test(bufType: String, enableSecure: Boolean) = runBlocking {
        val host = "localhost"
        val port = 4001
        val maxMsgCount = 10_000
        val connectionNum = 4
        val msgCount = AtomicInteger()
        val tcpConfig = TcpConfig(30, enableSecure)

        val server = AioTcpServer(tcpConfig).listen(host, port)
        val serverAcceptsConnJob = launchGlobal {
            val tcpConnChannel = server.tcpConnectionChannel
            acceptLoop@ while (true) {
                val connection = tcpConnChannel.receive()
                println("accept connection. ${connection.id}")
                connection.startReading()

                launchGlobal {
                    val inputChannel = connection.inputChannel

                    recvLoop@ while (true) {
                        val buf = inputChannel.receive()

                        readBufLoop@ while (buf.hasRemaining()) {
                            val num = buf.int
                            msgCount.incrementAndGet()
//                            println("server =======> $num")

                            val newBuf = ByteBuffer.allocate(4)
                            newBuf.putInt(num).flip()
                            connection.write(newBuf)
                        }
                    }
                }
            }
        }

        val client = AioTcpClient(tcpConfig)
        val time = measureTimeMillis {
            val maxCount = maxMsgCount / connectionNum
            val jobs = (1..connectionNum).map {
                launchGlobal {
                    val connection = client.connect(host, port).await()
                    println("create connection. ${connection.id}")
                    connection.startReading()

                    val readingJob = launchGlobal {
                        val inputChannel = connection.inputChannel
                        recvLoop@ while (true) {
                            val buf = inputChannel.receive()

                            readBufLoop@ while (buf.hasRemaining()) {
                                val num = buf.int
                                msgCount.incrementAndGet()
//                                println("client ------> $num")

                                if (num == maxCount) {
                                    connection.close()
                                    break@recvLoop
                                }
                            }
                        }
                    }

                    when (bufType) {
                        "single" -> {
                            (1..maxCount).forEach { i ->
                                val buf = ByteBuffer.allocate(4)
                                buf.putInt(i).flip()
                                connection.write(buf)
                            }
                        }
                        "array" -> {
                            val bufArray = Array<ByteBuffer>(maxCount) { index ->
                                val buf = ByteBuffer.allocate(4)
                                buf.putInt(index + 1).flip()
                                buf
                            }
                            connection.write(bufArray, 0, bufArray.size)
                        }
                        "list" -> {
                            val bufList = List<ByteBuffer>(maxCount) { index ->
                                val buf = ByteBuffer.allocate(4)
                                buf.putInt(index + 1).flip()
                                buf
                            }
                            connection.write(bufList, 0, bufList.size)
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

            assertEquals(maxMsgCount * 2, msgCount.get())
        }
        val throughput = maxMsgCount / (time / 1000.00)
        println("success. $time, $throughput")
//        println("success. $time")


        val stopTime = measureTimeMillis {
            serverAcceptsConnJob.cancel()
            server.stop()
            client.stop()
        }
        println("stop success. $stopTime")
    }

    @Test
    fun testTimeout() = runBlocking {
        val host = "localhost"
        val port = 4001
        val server = AioTcpServer(TcpConfig(1)).listen(host, port)
        val serverAcceptsConnJob = launchGlobal {
            val tcpConnChannel = server.tcpConnectionChannel
            acceptLoop@ while (true) {
                val conn = tcpConnChannel.receive()
                conn.startReading()
                delay(Duration.ofSeconds(2))
                assertTrue(conn.isClosed)
                break@acceptLoop
            }
        }

        val client = AioTcpClient(TcpConfig(1))
        val conn = client.connect(host, port).await()
        conn.startReading()
        assertEquals(port, conn.remoteAddress.port)
        delay(Duration.ofSeconds(2))
        assertTrue(conn.isClosed)
        assertTrue(conn.duration > 0)
        assertTrue(conn.closeTime > 0)

        val stopTime = measureTimeMillis {
            serverAcceptsConnJob.cancel()
            server.stop()
            client.stop()
        }
        println("stop success. $stopTime")
    }
}