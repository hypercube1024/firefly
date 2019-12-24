package com.fireflysource.common.track;

import com.fireflysource.common.coroutine.FinalizableScheduledExecutorService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Pengtao Qiu
 */
class TestFixedTimeLeakDetector {

    @Test
    void testFixedTimeLeakDetector() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean leaked = new AtomicBoolean(false);
        FixedTimeLeakDetector<TrackedObject> detector = new FixedTimeLeakDetector<>(
                new FinalizableScheduledExecutorService(Executors.newSingleThreadScheduledExecutor()),
                1, 1, 1, TimeUnit.SECONDS,
                () -> System.out.println("not any leaked object"));

        TrackedObject trackedObject = new TrackedObject();
        String name = "My tracked object 1";
        trackedObject.setName(name);
        detector.register(trackedObject, o -> {
            System.out.println(name + " leaked.");
            leaked.set(true);
            latch.countDown();
        });
        assertFalse(leaked.get());

        latch.await(10, TimeUnit.SECONDS);
        assertTrue(leaked.get());
    }

    static class TrackedObject {
        private boolean released;
        private String name;

        void release() {
            released = true;
        }

        boolean isReleased() {
            return released;
        }

        void setReleased(boolean released) {
            this.released = released;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

    }
}
