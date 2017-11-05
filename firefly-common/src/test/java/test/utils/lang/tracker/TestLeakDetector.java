package test.utils.lang.tracker;

import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.lang.tracker.LeakDetector;
import com.firefly.utils.lang.tracker.LeakDetectorReference;
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
        private LeakDetectorReference<TrackedObject> leakDetectorReference;

        public void release() {
            released = true;
            leakDetectorReference.clear();
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

        public LeakDetectorReference<TrackedObject> getLeakDetectorReference() {
            return leakDetectorReference;
        }

        public void setLeakDetectorReference(LeakDetectorReference<TrackedObject> leakDetectorReference) {
            this.leakDetectorReference = leakDetectorReference;
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
        LeakDetectorReference<TrackedObject> ref = leakDetector.create(trackedObject, () -> {
            System.out.println(name + " leaked.");
            leaked.set(true);
            phaser.arrive();
        });
        System.out.println(ref);
        trackedObject.setLeakDetectorReference(ref);
        Assert.assertFalse(leaked.get());
//        trackedObject.release();

        // Simulate the TrackedObject leaked. When the garbage collector cleans up the object, it is not released.
        trackedObject = null;
        System.gc();
        phaser.arriveAndAwaitAdvance();
        Assert.assertTrue(leaked.get());
    }
}
