package com.fireflysource.common.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Pengtao Qiu
 */
public class TestCompletableFutures {

    @Test
    @DisplayName("should retry operation successfully")
    void testRetrySuccessfully() throws Exception {
        AtomicInteger i = new AtomicInteger(4);
        CompletableFuture<String> future = CompletableFutures.retry(3, () -> {
            System.out.println("execute count: " + i.get());
            if (i.decrementAndGet() > 0) {
                return CompletableFutures.failedFuture(new IllegalStateException("error"));
            } else {
                return CompletableFuture.completedFuture("ok");
            }
        }, (e, c) -> System.out.println("start to retry: " + c));
        String str = future.get();
        assertEquals("ok", str);
        assertEquals(0, i.get());
    }

    @Test
    @DisplayName("should retry operation failure")
    void testRetryFailure() {
        AtomicInteger i = new AtomicInteger(4);
        CompletableFuture<String> future = CompletableFutures.retry(2, () -> {
            System.out.println("execute count: " + i.get());
            if (i.decrementAndGet() > 0) {
                return CompletableFutures.failedFuture(new IllegalStateException("error"));
            } else {
                return CompletableFuture.completedFuture("ok");
            }
        }, (e, c) -> System.out.println("start to retry: " + c));

        assertThrows(ExecutionException.class, () -> future.get());
    }
}
