package test.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.lang.pool.AsynchronousPool;
import com.firefly.utils.lang.pool.BoundedAsynchronousPool;
import com.firefly.utils.lang.pool.PooledObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestAsyncPool {

    @Test
    public void testBoundedAsynchronousPool() throws ExecutionException, InterruptedException {
        BoundedAsynchronousPool<TestPooledObject> pool = createPool(10);
        List<PooledObject<TestPooledObject>> list = new ArrayList<>();
        for (int j = 0; j < 4; j++) {
            PooledObject<TestPooledObject> o = pool.take().get();
            o.getObject().closed = true;
            list.add(o);
        }
        list.forEach(pool::release);

        list = new ArrayList<>();
        for (int j = 0; j < 3; j++) {
            PooledObject<TestPooledObject> o = pool.take().get();
            list.add(o);
        }
        list.forEach(pool::release);

        Assert.assertThat(pool.size(), is(pool.getCreatedObjectSize()));
        System.out.println(pool.size());
        Assert.assertThat(pool.size(), is(3));
        pool.stop();
    }

    @Test
    public void testAsynchronousPool() throws InterruptedException, ExecutionException {
        AsynchronousPool<TestPooledObject> pool = createPool(10);
        PooledObject<TestPooledObject> o = pool.take().get();
        Assert.assertThat(pool.size(), is(0));
        pool.release(o);
        Assert.assertThat(pool.size(), is(1));
        System.out.println("o -> " + o.getObject().i);

        PooledObject<TestPooledObject> o2 = pool.take().get();
        Assert.assertThat(o, is(o2));
        pool.release(o2);
        System.out.println("o2 -> " + o2.getObject().i);

        List<PooledObject<TestPooledObject>> list = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            PooledObject<TestPooledObject> o3 = pool.take().get();
            System.out.println(o3.getObject().i + "|" + o3.getObject().closed);
            list.add(o3);
        }
        list.forEach(pool::release);
        System.out.println("-------------------");
        for (int j = 0; j < 10; j++) {
            PooledObject<TestPooledObject> o3 = pool.take().get();
            System.out.println(o3.getObject().i + "|" + o3.getObject().closed);
        }

        pool.stop();
        Assert.assertThat(pool.size(), is(0));
    }

    @Test
    public void testCompletable() throws ExecutionException, InterruptedException {
        Promise.Completable<String> completable = new Promise.Completable<>();
        completable.succeeded("hello");
        completable.thenAccept(str -> Assert.assertThat(str, is("hello")));
        Assert.assertThat(completable.get(), is("hello"));
        Assert.assertThat(completable.get(), is("hello"));
        completable.thenAccept(str -> Assert.assertThat(str, is("hello")));
        completable.thenAccept(str -> Assert.assertThat(str, is("hello")));
        completable.thenAccept(str -> Assert.assertThat(str, is("hello")));
        completable.thenAccept(str -> Assert.assertThat(str, is("hello")));

        completable = new Promise.Completable<>();
        completable.exceptionally(t -> {
            System.out.println("failure1");
            Assert.assertThat(t, instanceOf(CommonRuntimeException.class));
            return "failure1";
        }).exceptionally(t -> {
            System.out.println("failure2");
            return "failure2";
        });
        completable.failed(new CommonRuntimeException("test"));
    }

    @Test
    public void testAtomic() {
        int maxSize = 8;
        AtomicInteger i = new AtomicInteger(0);
        for (int j = 0; j < maxSize + 5; j++) {
            final int k = j;
            int p = i.accumulateAndGet(maxSize, (prev, max) -> {
                if (prev < max) {
                    System.out.println("prev: " + prev + ", next: " + max);
                    Assert.assertThat(prev, is(k));
                    Assert.assertThat(max, is(maxSize));
                    return prev + 1;
                } else {
                    Assert.assertThat(max, is(maxSize));
                    return max;
                }
            });
            System.out.println("--------p: " + p);
        }
        System.out.println(">>>>>>>>>> " + i.get());
        System.out.println();

        for (int j = maxSize; j > 0 - 5; j--) {
            final int k = j;
            int p = i.getAndAccumulate(maxSize, (prev, max) -> {
                if (prev > 0) {
                    System.out.println("prev: " + prev + ", next: " + max);
                    Assert.assertThat(prev, is(k));
                    Assert.assertThat(max, is(maxSize));
                    return prev - 1;
                } else {
                    return 0;
                }
            });
            System.out.println("--------p: " + p);
        }

        System.out.println(i.get());

        AtomicBoolean bool = new AtomicBoolean(false);
        Assert.assertThat(bool.compareAndSet(false, true),  is(true));
        Assert.assertThat(bool.get(), is (true));
    }

    @Test
    public void testBoundedAsynchronousPoolException() throws InterruptedException {
        AtomicInteger i = new AtomicInteger();
        BoundedAsynchronousPool<TestPooledObject> pool = new BoundedAsynchronousPool<>(8, () -> {
            Promise.Completable<PooledObject<TestPooledObject>> completable = new Promise.Completable<>();
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
                    completable.succeeded(new PooledObject<>(new TestPooledObject(x)));
                }
            }).start();

            return completable;
        }, (o) -> !o.getObject().closed, (o) -> {
            System.out.println("destory obj - [" + o.getObject().i + "]");
            o.getObject().closed = true;
        });

        int number = 100;
        Phaser phaser = new Phaser(number + 1);
        for (int j = 0; j < number; j++) {
            pool.take()
                .thenAccept(o -> {
                    System.out.println("get o: " + o.getObject().i + "| created object size: " + pool.getCreatedObjectSize());
                    new Thread(() -> {
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        phaser.arrive();
                        pool.release(o);
                    }).start();
                })
                .exceptionally(t -> {
                    phaser.arrive();
                    System.out.println(t.getMessage());
                    Assert.assertThat(t, instanceOf(CommonRuntimeException.class));
                    return null;
                });
        }
        phaser.arriveAndAwaitAdvance();
        Assert.assertThat(pool.size(), is(6));
        Assert.assertThat(pool.size(), is(pool.getCreatedObjectSize()));
        System.out.println(pool.size());
        pool.stop();
    }

    private BoundedAsynchronousPool<TestPooledObject> createPool(int size) {
        AtomicInteger i = new AtomicInteger();
        return new BoundedAsynchronousPool<>(size, () -> {
            Promise.Completable<PooledObject<TestPooledObject>> completable = new Promise.Completable<>();
            completable.succeeded(new PooledObject<>(new TestPooledObject(i.getAndIncrement())));
            return completable;
        }, (o) -> !o.getObject().closed, (o) -> {
            System.out.println("destory obj - [" + o.getObject().i + "]");
            o.getObject().closed = true;
        });
    }

    public static class TestPooledObject {
        public boolean closed = false;
        public int i;

        public TestPooledObject(int i) {
            this.i = i;
        }

    }
}
