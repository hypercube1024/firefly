package test.utils.concurrent;

import com.firefly.utils.concurrent.IteratingCallback;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestIteratingCallback {

    @Test
    public void test() {
        int loop = 20;
        Queue<String> strings = new LinkedList<>();
        StringBuilder s = new StringBuilder(loop * 2);
        for (int i = 0; i < loop; i++) {
            strings.offer("e" + i);
            s.append("e").append(i);
        }

        StringBuilder ret = new StringBuilder(loop * 2);

        IteratingCallback callback = new IteratingCallback() {

            @Override
            protected Action process() {
                String e = strings.poll();
                if (e != null) {
                    ret.append(e);
                    return Action.SCHEDULED;
                } else {
                    return Action.IDLE;
                }
            }
        };
        callback.iterate();
        for (int i = 0; i < loop; i++) {
            callback.succeeded();
        }

        Assert.assertThat(ret.toString(), is(s.toString()));
    }
}
