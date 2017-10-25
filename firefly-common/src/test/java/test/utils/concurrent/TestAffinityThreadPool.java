package test.utils.concurrent;

import com.firefly.utils.concurrent.AffinityThreadPool;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestAffinityThreadPool {

    private int processorNumber = Runtime.getRuntime().availableProcessors();

    @Test
    public void test() throws InterruptedException, ExecutionException {
        AffinityThreadPool pool = new AffinityThreadPool();
        int maxTask = 20;
        List<Callable<String>> taskList = new ArrayList<>();
        for (int i = 0; i < maxTask; i++) {
            taskList.add(create(i));
        }
        List<Future<String>> futureList = pool.invokeAll(taskList);
        for (int i = 0; i < maxTask; i++) {
            Future<String> future = futureList.get(i);
            System.out.println(future.get());
            Assert.assertThat(future.get(), is("firefly-affinity-thread-" + (i % processorNumber)));
        }

        taskList = new ArrayList<>();
        int id = 3;
        for (int i = 0; i < maxTask; i++) {
            taskList.add(create(id));
        }
        futureList = pool.invokeAll(taskList);
        for (int i = 0; i < maxTask; i++) {
            Future<String> future = futureList.get(i);
            System.out.println(future.get());
            Assert.assertThat(future.get(), is("firefly-affinity-thread-" + (id % processorNumber)));
        }

        pool.shutdown();
        Assert.assertThat(pool.isShutdown(), is(true));
        boolean terminated = pool.awaitTermination(5, TimeUnit.SECONDS);
        Assert.assertThat(terminated, is(true));
    }

    private AffinityTask create(int id) {
        return new AffinityTask(id);
    }

    private class AffinityTask implements Callable<String> {

        private final int id;

        public AffinityTask(int id) {
            this.id = id;
        }

        @Override
        public String call() throws Exception {
            return Thread.currentThread().getName();
        }

        @Override
        public int hashCode() {
            return id;
        }

    }
}
