package test.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.lang.pool.AsynchronousPool;
import com.firefly.utils.lang.pool.BoundedAsynchronousPool;
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
    public void testBoundedAsynchronousPool() throws ExecutionException, InterruptedException {
        AtomicInteger i = new AtomicInteger();
        BoundedAsynchronousPool<PooledObject> pool = new BoundedAsynchronousPool<>(10, () -> {
            Promise.Completable<PooledObject> completable = new Promise.Completable<>();
            completable.succeeded(new PooledObject(i.getAndIncrement()));
            return completable;
        }, (o) -> !o.closed, (o) -> {
            System.out.println("destory obj - [" + o.i + "]");
            o.closed = true;
        });

        testAsynchronousPool(pool);

        System.out.println("----------------------");
        pool = new BoundedAsynchronousPool<>(10, () -> {
            Promise.Completable<PooledObject> completable = new Promise.Completable<>();
            completable.succeeded(new PooledObject(i.getAndIncrement()));
            return completable;
        }, (o) -> !o.closed, (o) -> {
            System.out.println("destory obj - [" + o.i + "]");
            o.closed = true;
        });

        List<PooledObject> list = new ArrayList<>();
        for (int j = 0; j < 4; j++) {
            PooledObject o = pool.take().get();
            o.closed = true;
            list.add(o);
        }
        list.forEach(pool::release);

        list = new ArrayList<>();
        for (int j = 0; j < 3; j++) {
            PooledObject o = pool.take().get();
            list.add(o);
        }
        list.forEach(pool::release);

        Assert.assertThat(pool.size(), is(pool.getCreatedObjectSize()));
        System.out.println(pool.size());
        Assert.assertThat(pool.size(), is(3));
        pool.stop();
    }

    private void testAsynchronousPool(AsynchronousPool<PooledObject> pool) throws InterruptedException, ExecutionException {
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

        testBoundedAsynchronousPoolException();
    }

    public static void testBoundedAsynchronousPoolException() throws InterruptedException {
        AtomicInteger i = new AtomicInteger();
        BoundedAsynchronousPool<PooledObject> pool = new BoundedAsynchronousPool<>(8, () -> {
            Promise.Completable<PooledObject> completable = new Promise.Completable<>();
            new Thread(() -> {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int x = i.incrementAndGet();
                if (x % 3 == 0) {
                    completable.failed(new CommonRuntimeException("test create pooled object: " + x + " exception"));
                } else {
                    completable.succeeded(new PooledObject(x));
                }
            }).start();

            return completable;
        }, (o) -> !o.closed, (o) -> {
            System.out.println("destory obj - [" + o.i + "]");
            o.closed = true;
        });

        for (int j = 0; j < 20; j++) {
            pool.take()
                .thenAccept(o -> {
                    System.out.println("get o: " + o.i);
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        pool.release(o);
                    }).start();
                })
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
        }
        Thread.sleep(5000L);
    }

    public static class PooledObject {
        public boolean closed = false;
        public int i;

        public PooledObject(int i) {
            this.i = i;
        }

    }
}
