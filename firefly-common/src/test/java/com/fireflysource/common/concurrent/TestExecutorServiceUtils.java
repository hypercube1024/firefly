package com.fireflysource.common.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.fireflysource.common.concurrent.ExecutorServiceUtils.shutdownAndAwaitTermination;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Pengtao Qiu
 */
class TestExecutorServiceUtils {

    @Test
    void testShutdownAndAwaitTermination() {
        int threadNum = 2;
        int count = 10;
        long taskTime = 500L;
        long maxTime = taskTime * count / threadNum;

        ExecutorService pool = Executors.newFixedThreadPool(threadNum);
        AtomicInteger maxTask = new AtomicInteger(count);
        for (int i = 0; i < count; i++) {
            pool.submit(() -> {
                try {
                    Thread.sleep(taskTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("task complete. " + maxTask.getAndDecrement());
            });
        }
        shutdownAndAwaitTermination(pool, maxTime + 100, TimeUnit.MILLISECONDS);
        assertEquals(0, maxTask.get());
    }
}
