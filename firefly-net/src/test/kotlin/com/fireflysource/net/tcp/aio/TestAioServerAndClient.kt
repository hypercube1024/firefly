package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.launchWithAttr
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis

/**
 * @author Pengtao Qiu
 */
class TestAioServerAndClient {

    @Test
    fun test() = runBlocking {
        val server = AioTcpServer().onAccept { conn ->
            conn.startAutomaticReading()
            launchWithAttr {
                val channel = conn.inputChannel
                while (!conn.isShutdownInput) {
                    val buf = channel.receive()
                    val num = buf.int
                    println("server receive: $num")
                    buf.flip()
                    conn.write(buf)
                    if (num == 10) {
                        conn.close()
                        break
                    }
                }
            }
        }
        server.listen("localhost", 4001)
        val client = AioTcpClient()

        val time = measureTimeMillis {
            val conn = client.connect("localhost", 4001).await()
            (1..10).forEach { i ->
                val buf = ByteBuffer.allocate(4)
                buf.putInt(i).flip()
                conn.write(buf)
            }
            conn.startAutomaticReading()
            val readingJob = launchWithAttr {
                val channel = conn.inputChannel
                while (!conn.isShutdownInput) {
                    val buf = channel.receive()
                    val num = buf.int
                    println("client receive: $num")
                    buf.flip()
                    conn.write(buf)
                    if (num == 10) {
                        conn.close()
                        break
                    }
                }
            }
            readingJob.join()
        }
        println("success. $time")

        val stopTime = measureTimeMillis {
            server.stop()
            client.stop()
        }
        println("stop success. $stopTime")
    }
}