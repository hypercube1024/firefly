package test.utils.concurrent;

import com.firefly.utils.concurrent.Atomics;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestAtomics {

    @Test
    public void test() {
        int max = 8;
        int min = 0;
        AtomicInteger i = new AtomicInteger(0);

        for (int j = 0; j < max + 10; j++) {
            int p = Atomics.getAndIncrement(i, max);
            if (j < max) {
                Assert.assertThat(p, is(j));
            } else {
                Assert.assertThat(p, is(max));
            }
        }

        for (int j = max; j > -10; j--) {
            int p = Atomics.getAndDecrement(i, min);
            if (j > min) {
                Assert.assertThat(p, is(j));
            } else {
                Assert.assertThat(p, is(min));
            }
        }

    }
}
