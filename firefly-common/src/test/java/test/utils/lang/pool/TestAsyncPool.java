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
        list.forEach(PooledObject::release);

        list = new ArrayList<>();
        for (int j = 0; j < 3; j++) {
            PooledObject<TestPooledObject> o = pool.take().get();
            list.add(o);
        }
        list.forEach(PooledObject::release);

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
        o.release();
        Assert.assertThat(pool.size(), is(1));
        System.out.println("o -> " + o.getObject().i);

        PooledObject<TestPooledObject> o2 = pool.take().get();
        Assert.assertThat(o, is(o2));
        o2.release();
        System.out.println("o2 -> " + o2.getObject().i);

        List<PooledObject<TestPooledObject>> list = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            PooledObject<TestPooledObject> o3 = pool.take().get();
            System.out.println(o3.getObject().i + "|" + o3.getObject().closed);
            list.add(o3);
        }
        list.forEach(PooledObject::release);
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
        BoundedAsynchronousPool<TestPooledObject> pool = new BoundedAsynchronousPool<>(maxSize, p -> {
            Promise.Completable<PooledObject<TestPooledObject>> completable = new Promise.Completable<>();
            new Thread(() -> {
                ThreadUtils.sleep(100L);
                int x = i.incrementAndGet();
                TestPooledObject obj = new TestPooledObject(x);
                queue.offer(obj);
                completable.succeeded(new PooledObject<>(obj, p));
            }).start();
            return completable;
        }, o -> !o.getObject().closed, o -> {
            System.out.println("destroy obj - [" + o.getObject().i + "]");
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
        int createdSize = pool.getCreatedObjectSize();
        Assert.assertThat(poolSize, lessThanOrEqualTo(maxSize));
        Assert.assertThat(createdSize, lessThanOrEqualTo(maxSize));
        System.out.println("pool size: " + poolSize + ", created size: " + createdSize);
        start.set(false);
        pool.stop();
    }

    @Test
    public void testBoundedAsynchronousPoolException() throws InterruptedException {
        AtomicInteger i = new AtomicInteger();
        BoundedAsynchronousPool<TestPooledObject> pool = new BoundedAsynchronousPool<>(8, p -> {
            Promise.Completable<PooledObject<TestPooledObject>> completable = new Promise.Completable<>();
            new Thread(() -> {
                ThreadUtils.sleep(100L);
                int x = i.incrementAndGet();
                if (x % 3 == 0) {
                    completable.failed(new CommonRuntimeException("test create pooled object: " + x + " exception"));
                } else {
                    completable.succeeded(new PooledObject<>(new TestPooledObject(x), p));
                }
            }).start();
            return completable;
        }, o -> !o.getObject().closed, o -> {
            System.out.println("destroy obj - [" + o.getObject().i + "]");
            o.getObject().closed = true;
        });

        int number = 100;
        takeObjectTest(pool, number);

        int poolSize = pool.size();
        int createdSize = pool.getCreatedObjectSize();
        Assert.assertThat(poolSize, lessThanOrEqualTo(8));
        Assert.assertThat(createdSize, lessThanOrEqualTo(8));
        System.out.println("pool size: " + poolSize + ", created size: " + createdSize);
        pool.stop();
    }

    private void takeObjectTest(BoundedAsynchronousPool<TestPooledObject> pool, int number) {
        CountDownLatch countDownLatch = new CountDownLatch(number);
        for (int j = 0; j < number; j++) {
            pool.take()
                .thenAccept(o -> {
                    System.out.println("get o: " + o.getObject().i + "| created object size: " + pool.getCreatedObjectSize());
                    new Thread(() -> {
                        ThreadUtils.sleep(100L);
                        countDownLatch.countDown();
                        o.release();
                    }).start();
                })
                .exceptionally(t -> {
                    System.out.println(t.getMessage() + "|" + t.getClass());
                    countDownLatch.countDown();
                    return null;
                });
            System.out.println("loop num -> " + j);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private BoundedAsynchronousPool<TestPooledObject> createPool(int size) {
        AtomicInteger i = new AtomicInteger();
        return new BoundedAsynchronousPool<>(size, pool -> {
            Promise.Completable<PooledObject<TestPooledObject>> completable = new Promise.Completable<>();
            completable.succeeded(new PooledObject<>(new TestPooledObject(i.getAndIncrement()), pool));
            return completable;
        }, o -> !o.getObject().closed, o -> {
            System.out.println("destroy obj - [" + o.getObject().i + "]");
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
