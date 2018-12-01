package com.fireflysource.common.track;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pengtao Qiu
 */
public class TestFixedTimeLeakDetector {

    public static class TrackedObject {
        private boolean released;
        private String name;

        public void release() {
            released = true;
        }

        public boolean isReleased() {
            return released;
        }

        public void setReleased(boolean released) {
            this.released = released;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    @Test
    public void testFixedTimeLeakDetector() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean leaked = new AtomicBoolean(false);
        FixedTimeLeakDetector<TrackedObject> detector = new FixedTimeLeakDetector<>(1, 1,
                () -> System.out.println("not any leaked object"));

        TrackedObject trackedObject = new TrackedObject();
        String name = "My tracked object 1";
        trackedObject.setName(name);
        detector.register(trackedObject, o -> {
            System.out.println(name + " leaked.");
            leaked.set(true);
            latch.countDown();
        });
        Assert.assertFalse(leaked.get());

        latch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(leaked.get());
    }
}
