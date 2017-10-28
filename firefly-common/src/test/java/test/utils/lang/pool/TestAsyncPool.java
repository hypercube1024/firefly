package test.utils.lang.pool;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.lang.pool.AsynchronousPool;
import com.firefly.utils.lang.pool.BoundedAsynchronousPool;
import com.firefly.utils.lang.pool.PooledObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;

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

        int poolSize = pool.size();
        Assert.assertThat(poolSize, is(pool.getCreatedObjectSize()));
        System.out.println(poolSize);
        Assert.assertThat(poolSize, is(4));
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
    public void testInvalid() throws Exception {
        int maxSize = 8;
        AtomicInteger i = new AtomicInteger();
        AtomicBoolean start = new AtomicBoolean(true);
        TransferQueue<TestPooledObject> queue = new LinkedTransferQueue<>();
        BoundedAsynchronousPool<TestPooledObject> pool = new BoundedAsynchronousPool<>(maxSize, () -> {
            Promise.Completable<PooledObject<TestPooledObject>> completable = new Promise.Completable<>();
            new Thread(() -> {
                ThreadUtils.sleep(100L);
                int x = i.incrementAndGet();
                TestPooledObject obj = new TestPooledObject(x);
                queue.offer(obj);
                completable.succeeded(new PooledObject<>(obj));
            }).start();
            return completable;
        }, (o) -> !o.getObject().closed, (o) -> {
            System.out.println("destory obj - [" + o.getObject().i + "]");
            o.getObject().closed = true;
        });
        new Thread(() -> {

            while (start.get()) {
                try {
                    TestPooledObject obj = queue.poll(5, TimeUnit.SECONDS);
                    if (obj != null) {
                        ThreadUtils.sleep(100L);
                        obj.closed = true;
                    }
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();

        int number = 100;
        takeObjectTest(pool, number);

        int poolSize = pool.size();
        Assert.assertThat(poolSize, lessThanOrEqualTo(maxSize));
        Assert.assertThat(pool.getCreatedObjectSize(), lessThanOrEqualTo(maxSize));
        System.out.println(poolSize);
        start.set(false);
        pool.stop();
    }

    @Test
    public void testBoundedAsynchronousPoolException() throws InterruptedException {
        AtomicInteger i = new AtomicInteger();
        BoundedAsynchronousPool<TestPooledObject> pool = new BoundedAsynchronousPool<>(8, () -> {
            Promise.Completable<PooledObject<TestPooledObject>> completable = new Promise.Completable<>();
            new Thread(() -> {
                ThreadUtils.sleep(100L);
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
        takeObjectTest(pool, number);

        int poolSize = pool.size();
        Assert.assertThat(poolSize, lessThanOrEqualTo(8));
        Assert.assertThat(pool.getCreatedObjectSize(), lessThanOrEqualTo(8));
        System.out.println(poolSize);
        pool.stop();
    }

    private void takeObjectTest(BoundedAsynchronousPool<TestPooledObject> pool, int number) {
        Phaser phaser = new Phaser(number + 1);
        for (int j = 0; j < number; j++) {
            pool.take()
                .thenAccept(o -> {
                    System.out.println("get o: " + o.getObject().i + "| created object size: " + pool.getCreatedObjectSize());
                    new Thread(() -> {
                        ThreadUtils.sleep(100L);
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
            System.out.println("loop num -> " + j);
        }
        phaser.arriveAndAwaitAdvance();
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
        public volatile boolean closed = false;
        public final int i;

        public TestPooledObject(int i) {
            this.i = i;
        }

    }
}
