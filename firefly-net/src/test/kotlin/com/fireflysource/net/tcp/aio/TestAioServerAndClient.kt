package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.CoroutineDispatchers.singleThread
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
        val maxMsgCount = 200
        val msgCount = AtomicInteger()

        val server = AioTcpServer().listen(host, port)
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
//                            println("server receive ---> $num")
                            msgCount.incrementAndGet()

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


        val client = AioTcpClient()
        val time = measureTimeMillis {
            val conn = client.connect(host, port).await()
            (1..maxMsgCount).forEach { i ->
                val buf = ByteBuffer.allocate(4)
                buf.putInt(i).flip()
                conn.write(buf)
            }
            conn.startReading()
            val readingJob = launchWithAttr(singleThread) {
                val inputChannel = conn.inputChannel
                recvLoop@ while (true) {
                    val buf = inputChannel.receive()

                    readBufLoop@ while (buf.hasRemaining()) {
                        val num = buf.int
//                        println("client receive ***> $num")
                        msgCount.incrementAndGet()
                        if (num == maxMsgCount) {
                            conn.close()
                            break@recvLoop
                        }
                    }
                }
            }

            readingJob.join()
            assertEquals(maxMsgCount * 4L, conn.readBytes)
            assertEquals(maxMsgCount * 4L, conn.writtenBytes)
            assertEquals(maxMsgCount * 2, msgCount.get())
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
}