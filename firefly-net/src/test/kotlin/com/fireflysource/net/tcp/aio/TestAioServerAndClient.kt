package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.launchWithAttr
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * @author Pengtao Qiu
 */
class TestAioServerAndClient {

    @Test
    fun test() = runBlocking {
        val host = "localhost"
        val port = 4001
        val maxMsgCount = 10
        val server = AioTcpServer()
        server.listen(host, port)
        val client = AioTcpClient()

        val msgCount = AtomicInteger()
        val time = measureTimeMillis {
            val serverAcceptsConnJob = launchWithAttr {
                val tcpConnChannel = server.tcpConnectionChannel
                acceptLoop@ while (true) {
                    val conn = tcpConnChannel.receive()
                    conn.startAutomaticReading()

                    launchWithAttr {
                        val inputChannel = conn.inputChannel

                        recvLoop@ while (true) {
                            val buf = inputChannel.receive()

                            readBufLoop@ while (buf.hasRemaining()) {
                                val num = buf.int
                                println("server receive ---> $num")
                                msgCount.incrementAndGet()

                                val newBuf = ByteBuffer.allocate(4)
                                newBuf.putInt(num).flip()
                                conn.write(newBuf)
                                if (num == 10) {
                                    break@recvLoop
                                }
                            }
                        }
                    }
                }

            }

            val conn = client.connect(host, port).await()
            (1..maxMsgCount).forEach { i ->
                val buf = ByteBuffer.allocate(4)
                buf.putInt(i).flip()
                conn.write(buf)
            }
            conn.startAutomaticReading()
            val readingJob = launchWithAttr {
                val inputChannel = conn.inputChannel
                recvLoop@ while (true) {
                    val buf = inputChannel.receive()

                    readBufLoop@ while (buf.hasRemaining()) {
                        val num = buf.int
                        println("client receive ***> $num")
                        msgCount.incrementAndGet()
                        if (num == 10) {
                            conn.close()
                            break@recvLoop
                        }
                    }
                }
            }

            readingJob.join()
            assertEquals(maxMsgCount * 2, msgCount.get())
            serverAcceptsConnJob.cancel()
        }
        println("success. $time")

        val stopTime = measureTimeMillis {
            server.stop()
            client.stop()
        }
        println("stop success. $stopTime")
    }
}