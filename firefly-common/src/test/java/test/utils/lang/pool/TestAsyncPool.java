package test.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.lang.pool.ThreadLocalAsynchronousPool;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestAsyncPool {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        AtomicInteger i = new AtomicInteger();
        ThreadLocalAsynchronousPool<PooledObject> pool = new ThreadLocalAsynchronousPool<>(() -> {
            Promise.Completable<PooledObject> completable = new Promise.Completable<>();
            completable.succeeded(new PooledObject(i.getAndIncrement()));
            return completable;
        }, (o) -> !o.closed, (o) -> {
            System.out.println("destory obj - [" + o.i + "]");
            o.closed = true;
        });

        PooledObject o = pool.take().get();
        Assert.assertThat(pool.size(), is(0));
        pool.release(o);
        Assert.assertThat(pool.size(), is(1));
        System.out.println("o -> " + o.i);

        PooledObject o2 = pool.take().get();
        Assert.assertThat(o, is(o2));
        pool.release(o2);
        System.out.println("o2 -> " + o2.i);

        List<PooledObject> list = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            PooledObject o3 = pool.take().get();
            System.out.println(o3.i + "|" + o3.closed);
            list.add(o3);
        }
        list.forEach(pool::release);
        System.out.println("-------------------");
        for (int j = 0; j < 10; j++) {
            PooledObject o3 = pool.take().get();
            System.out.println(o3.i + "|" + o3.closed);
        }

        pool.stop();
        Assert.assertThat(pool.size(), is(0));
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Promise.Completable<String> completable = new Promise.Completable<>();
        completable.succeeded("hello");
        completable.thenAccept(System.out::println);
        System.out.println(completable.get());
        System.out.println(completable.get());
        completable.thenAccept(System.out::println);
        completable.thenAccept(System.out::println);
        completable.thenAccept(System.out::println);
        completable.thenAccept(System.out::println);


        completable = new Promise.Completable<>();
        completable.exceptionally(t -> {
            System.out.println("failure1");
            return "failure1";
        }).exceptionally(t -> {
            System.out.println("failure2");
            return "failure2";
        });
        completable.failed(new CommonRuntimeException("test"));
    }

    public static class PooledObject {
        public boolean closed = false;
        public int i;

        public PooledObject(int i) {
            this.i = i;
        }

    }
}
