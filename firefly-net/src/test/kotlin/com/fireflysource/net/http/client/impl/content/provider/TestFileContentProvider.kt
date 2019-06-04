package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.aWrite
import com.fireflysource.net.http.client.HttpClientContentProviderFactory.createFileContentProvider
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE

class TestFileContentProvider {

    private val tmpFile = Paths.get(System.getProperty("user.home"), "tmpFile.txt")

    @BeforeEach
    fun init() {
        Files.createFile(tmpFile)
        println("create file: $tmpFile")
    }

    @AfterEach
    fun destroy() {
        Files.delete(tmpFile)
        println("delete file: $tmpFile")
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun test() = runBlocking {
        val capacity = 24

        AsynchronousFileChannel.open(tmpFile, WRITE).use {
            val buffer = BufferUtils.allocate(capacity)
            val pos = BufferUtils.flipToFill(buffer)
            buffer.putInt(1).putInt(2).putInt(3)
                .putInt(4).putInt(5).putInt(6)
            BufferUtils.flipToFlush(buffer, pos)

            val len = it.aWrite(buffer, 0L)
            assertEquals(capacity, len)
        }

        createFileContentProvider(tmpFile, READ).use { provider ->
            val buffer = BufferUtils.allocate(capacity)
            val pos = BufferUtils.flipToFill(buffer)
            val len = provider.read(buffer).await()
            BufferUtils.flipToFlush(buffer, pos)
            assertEquals(capacity, len)

            (1..6).forEach { i ->
                assertEquals(i, buffer.int)
            }
        }
    }
}