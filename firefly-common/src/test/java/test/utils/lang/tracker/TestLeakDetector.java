package test.utils.lang.tracker;

import com.firefly.utils.lang.LeakDetector;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Phaser;
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
    public void test() {
        Phaser phaser = new Phaser(2);
        AtomicBoolean leaked = new AtomicBoolean(false);
        LeakDetector<TrackedObject> leakDetector = new LeakDetector<>(0L, 1L,
                () -> System.out.println("not any leaked object"));

        TrackedObject trackedObject = new TrackedObject();
        String name = "My tracked object 1";
        trackedObject.setName(name);

        leakDetector.register(trackedObject, () -> {
            System.out.println(name + " leaked.");
            leaked.set(true);
            phaser.arrive();
        });
        Assert.assertFalse(leaked.get());
//        trackedObject.release();

        // Simulate the TrackedObject leaked. When the garbage collector cleans up the object, it is not released.
        trackedObject = null;
        System.gc();
        phaser.arriveAndAwaitAdvance();
        Assert.assertTrue(leaked.get());
    }
}
