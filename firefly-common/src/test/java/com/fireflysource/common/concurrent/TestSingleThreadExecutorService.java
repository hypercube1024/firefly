package com.fireflysource.common.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSingleThreadExecutorService {

    @Test
    void test() {
        ExecutorService executorService = new SingleThreadExecutorService(1024 * 16);
        executorService.execute(() -> {
            try {
                System.out.println("start to execute.");
                Thread.sleep(1000L);
                System.out.println("execute success.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        ExecutorServiceUtils.shutdownAndAwaitTermination(executorService, 3, TimeUnit.SECONDS);
        assertTrue(executorService.isShutdown());
        assertTrue(executorService.isTerminated());
    }
}
