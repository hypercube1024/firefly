package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.CoroutineDispatchers.singleThread
import com.fireflysource.common.coroutine.launchWithAttr
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ValueSource
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import java.util.Arrays
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


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
        val msgCount = AtomicInteger()
        val tcpConfig = TcpConfig(30, enableSecure)

        val server = AioTcpServer(tcpConfig).listen(host, port)
        val serverAcceptsConnJob = launchWithAttr(singleThread) {
            val tcpConnChannel = server.tcpConnectionChannel
            acceptLoop@ while (true) {
                val conn = tcpConnChannel.receive()
                conn.startReading()

                launchWithAttr(singleThread) {
                    val inputChannel = conn.inputChannel

                    recvLoop@ while (true) {
                        val buf = inputChannel.receive()

                        readBufLoop@ while (buf.hasRemaining()) {
                            val num = buf.int
                            msgCount.incrementAndGet()
                            println("server =======> $num")

                            val newBuf = ByteBuffer.allocate(4)
                            newBuf.putInt(num).flip()
                            conn.write(newBuf)
                            if (num == maxMsgCount) {
                                break@recvLoop
                            }
                        }
                    }
                }
            }
        }


        val client = AioTcpClient(tcpConfig)
        val time = measureTimeMillis {
            val conn = client.connect(host, port).await()
            conn.startReading()

            val readingJob = launchWithAttr(singleThread) {
                val inputChannel = conn.inputChannel
                recvLoop@ while (true) {
                    val buf = inputChannel.receive()

                    readBufLoop@ while (buf.hasRemaining()) {
                        val num = buf.int
                        msgCount.incrementAndGet()
                        println("client -------> $num")
                        if (num == maxMsgCount) {
                            conn.close()
                            break@recvLoop
                        }
                    }
                }
            }

            when (bufType) {
                "single" -> {
                    (1..maxMsgCount).forEach { i ->
                        val buf = ByteBuffer.allocate(4)
                        buf.putInt(i).flip()
                        conn.write(buf)
                    }
                }
                "array" -> {
                    val bufArray = Array<ByteBuffer>(maxMsgCount) { index ->
                        val buf = ByteBuffer.allocate(4)
                        buf.putInt(index + 1).flip()
                        buf
                    }
                    conn.write(bufArray, 0, bufArray.size)
                }
                "list" -> {
                    val bufList = List<ByteBuffer>(maxMsgCount) { index ->
                        val buf = ByteBuffer.allocate(4)
                        buf.putInt(index + 1).flip()
                        buf
                    }
                    conn.write(bufList, 0, bufList.size)
                }
            }



            readingJob.join()
            assertEquals(maxMsgCount * 2, msgCount.get())
            println("conn info: ${conn.lastActiveTime}, ${conn.lastReadTime}, ${conn.lastWrittenTime}")
            assertTrue(conn.readBytes > 0)
            assertTrue(conn.writtenBytes > 0)
            assertTrue(conn.lastActiveTime > 0L)
            assertTrue(conn.lastReadTime > 0L)
            assertTrue(conn.lastWrittenTime > 0L)
        }
//        val throughput = maxMsgCount / (time / 1000)
//        println("success. $time, $throughput")
        println("success. $time")


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
        val serverAcceptsConnJob = launchWithAttr(singleThread) {
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