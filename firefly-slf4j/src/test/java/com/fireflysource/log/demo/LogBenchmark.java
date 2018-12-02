package com.fireflysource.log.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class LogBenchmark {

    private static final Logger log = LoggerFactory.getLogger("test-INFO");

    public static void main(String[] args) throws InterruptedException {
        test(4, 5_000_000, 20);
    }

    public static void test(int threadNum, int messageNum, int messageSize) throws InterruptedException {
        StringBuilder data = new StringBuilder(messageSize);
        for (int i = 0; i < messageSize; i++) {
            data.append("a");
        }
        String str = data.toString();

        CountDownLatch latch = new CountDownLatch(threadNum);

        Thread[] threads = new Thread[threadNum];
        int size = messageNum / threadNum;
        System.out.println("size: " + size);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < size; j++) {
                    log.info(str);
                }
//				System.out.println(Thread.currentThread().getName() + " arrived");
                latch.countDown();
            }, "test-thread-" + i);
        }

        long start = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        latch.await();
        long end = System.currentTimeMillis();
        long time = (end - start) / 1000;
        System.out.println("time: " + time);
        System.out.println("msg/sec: " + (messageNum / time));
    }

}
