package test.utils.concurrent;

import com.firefly.utils.concurrent.AffinityThreadPool;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestAffinityThreadPool {

    @Test
    public void test() throws InterruptedException, ExecutionException {
        AffinityThreadPool pool = new AffinityThreadPool();

        List<Callable<String>> taskList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            taskList.add(create(1));
        }
        List<Future<String>> futureList = pool.invokeAll(taskList);
        for (Future<String> future : futureList) {
            System.out.println(future.get());
            Assert.assertThat(future.get(), is("firefly-affinity-thread-1"));
        }

        if (Runtime.getRuntime().availableProcessors() >= 2) {
            taskList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                taskList.add(create(2));
            }
            futureList = pool.invokeAll(taskList);
            for (Future<String> future : futureList) {
                System.out.println(future.get());
                Assert.assertThat(future.get(), is("firefly-affinity-thread-2"));
            }
        }
    }

    private Callable<String> create(int id) {
        return new Callable<String>() {

            @Override
            public String call() throws Exception {
                return Thread.currentThread().getName();
            }

            @Override
            public int hashCode() {
                return id;
            }
        };
    }
}
