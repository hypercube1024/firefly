package com.fireflysource.common.actor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static com.fireflysource.common.actor.BlockingTask.runBlockingTask;

public class TestActor {

    @Test
    void test() throws InterruptedException {
        HelloActor helloActor = new HelloActor();
        for (int i = 0; i < 5; i++) {
            helloActor.send("task " + i);
        }
        helloActor.send("complete");
        helloActor.send("task 999");
        helloActor.send("task 1000");
        Thread.sleep(6000);
    }

    public static class HelloActor extends AbstractActor<String> {

        @Override
        public void onReceive(String message) {
            System.out.println(Thread.currentThread().getName() + " -- process " + message);
            if (message.equals("complete")) {
                shutdown();
            } else {
                pause();
                CompletableFuture.runAsync(() -> runBlockingTask(() -> Thread.sleep(1000)))
                                 .handle((v, e) -> {
                                     resume();
                                     return CompletableFuture.completedFuture(null);
                                 });
            }
        }

        @Override
        public void onDiscard(String message) {
            System.out.println(Thread.currentThread().getName() + " -- discard message: " + message);
        }
    }
}
