package com.fireflysource.common.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    @DisplayName("should do finally always")
    void testDoFinally() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "OK");
        CompletableFuture<String> result = CompletableFutures.doFinally(future, (value, ex) -> CompletableFuture.runAsync(() -> {
            String msg = "do finally. " + value;
            System.out.println(msg);
            ref.set(msg);
        }));
        assertEquals("OK", result.get());
        assertEquals("do finally. OK", ref.get());

        CompletableFuture<String> failure = CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("Failure");
        });
        CompletableFuture<String> failureResult = CompletableFutures.doFinally(failure, (value, ex) -> CompletableFuture.runAsync(() -> {
            String msg = "do finally. " + ex.getMessage();
            System.out.println(msg);
            ref.set(msg);
        }));
        assertThrows(ExecutionException.class, failureResult::get);
        assertEquals("do finally. java.lang.IllegalStateException: Failure", ref.get());
    }
}
