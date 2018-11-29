package test.utils.lang;

import com.firefly.utils.lang.track.PhantomReferenceLeakDetector;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pengtao Qiu
 */
public class TestLeakDetector {

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
    public void testLeak() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean leaked = new AtomicBoolean(false);
        PhantomReferenceLeakDetector<TrackedObject> leakDetector = new PhantomReferenceLeakDetector<>(0L, 1L,
                () -> System.out.println("not any leaked object"));

        TrackedObject trackedObject = new TrackedObject();
        String name = "My tracked object 1";
        trackedObject.setName(name);

        leakDetector.register(trackedObject, () -> {
            System.out.println(name + " leaked.");
            leaked.set(true);
            latch.countDown();
        });
        Assert.assertFalse(leaked.get());

        // Simulate the TrackedObject leaked. When the garbage collector cleans up the object, it is not released.
        trackedObject = null;
        System.gc();
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(leaked.get());
    }
}
