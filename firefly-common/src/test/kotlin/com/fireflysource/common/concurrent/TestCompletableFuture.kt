package com.fireflysource.common.concurrent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class TestCompletableFuture {

    @Test
    @DisplayName("should receive the exception message.")
    fun testException() {
        CompletableFuture
            .runAsync { throw IllegalStateException("test_error") }
            .exceptionallyAccept { assertEquals("test_error", it.cause?.message) }
            .get()
    }

    @Test
    @DisplayName("should exceptionally compose successfully.")
    fun testExceptionCompose() {
        val value = CompletableFuture
            .supplyAsync<String> { throw IllegalStateException("test_error") }
            .exceptionallyCompose { CompletableFuture.supplyAsync { "ok" } }
            .get()
        assertEquals("ok", value)
    }
}