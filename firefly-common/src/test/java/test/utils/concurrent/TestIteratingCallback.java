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
            if (i == (loop - 1)) {
                strings.offer("end");
            } else {
                strings.offer("e" + i);
                s.append("e").append(i);
            }
        }
        strings.offer("another");

        StringBuilder ret = new StringBuilder(loop * 2);

        IteratingCallback callback = new IteratingCallback() {

            @Override
            protected Action process() {
                String e = strings.poll();
                if (e != null) {
                    System.out.println("current element: " + e);
                    if (e.equals("end")) {
                        return Action.SUCCEEDED;
                    } else {
                        ret.append(e);
                        return Action.SCHEDULED;
                    }
                } else {
                    return Action.IDLE;
                }
            }

            @Override
            protected void onCompleteSuccess() {
                System.out.println("The tasks complete. Remaining element: " + strings.size());
                Assert.assertThat(strings.size(), is(1));
                strings.clear();
            }
        };

        callback.iterate();
        for (int i = 0; i < loop - 1; i++) {
            callback.succeeded();
        }

        Assert.assertThat(strings.size(), is(0));
        Assert.assertThat(ret.toString(), is(s.toString()));
    }
}
