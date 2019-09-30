package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.net.http.client.HttpClientResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.WRITE
import java.util.*

class TestFileContentHandler {

    private val response = Mockito.mock(HttpClientResponse::class.java)

    private val tmpFile = Paths.get(System.getProperty("user.home"), "tmpFile${UUID.randomUUID()}.txt")

    @BeforeEach
    fun init() {
        Files.createFile(tmpFile)
        println("create a file: $tmpFile")
    }

    @AfterEach
    fun destroy() {
        Files.delete(tmpFile)
        println("delete file: $tmpFile")
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun test() = runBlocking {
        val handler = FileContentHandler(tmpFile, WRITE)
        arrayOf(
            ByteBuffer.wrap("hello".toByteArray()),
            ByteBuffer.wrap(" file".toByteArray()),
            ByteBuffer.wrap(" handler".toByteArray())
        ).forEach { handler.accept(it, response) }

        handler.closeAwait()

        assertEquals("hello file handler", String(Files.readAllBytes(tmpFile)))
    }
}