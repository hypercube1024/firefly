package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.openFileChannelAsync
import com.fireflysource.common.io.useAwait
import com.fireflysource.common.io.writeAwait
import com.fireflysource.net.http.client.HttpClientContentProviderFactory.createFileContentProvider
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import java.util.*

class TestFileContentProvider {

    private val tmpFile = Paths.get(System.getProperty("user.home"), "tmpFile${UUID.randomUUID()}.txt")

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

    @Test
    @DisplayName("should read file successfully")
    fun test(): Unit = runBlocking {
        val capacity = 24
        val fileChannel = openFileChannelAsync(tmpFile, WRITE).await()
        fileChannel.useAwait {
            val writeBuffer = BufferUtils.allocate(capacity)
            val writePos = BufferUtils.flipToFill(writeBuffer)
            writeBuffer.putInt(1).putInt(2).putInt(3)
                .putInt(4).putInt(5).putInt(6)
            BufferUtils.flipToFlush(writeBuffer, writePos)

            val writeLen = fileChannel.writeAwait(writeBuffer, 0L)
            assertEquals(capacity, writeLen)
        }

        val provider = createFileContentProvider(tmpFile, READ) as FileContentProvider
        val readBuffer = BufferUtils.allocate(capacity)
        val readPos = BufferUtils.flipToFill(readBuffer)
        val readLen = provider.read(readBuffer).await()
        BufferUtils.flipToFlush(readBuffer, readPos)
        assertEquals(capacity, readLen)

        (1..6).forEach { i ->
            assertEquals(i, readBuffer.int)
        }
        provider.closeFuture().await()
        Unit
    }
}