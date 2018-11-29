package test.utils.lang.pool;

import com.firefly.utils.lang.pool.BoundObjectPool;
import com.firefly.utils.lang.pool.Pool;
import com.firefly.utils.lang.pool.PooledObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestBoundObjectPool {

    private AtomicInteger idGenerator = new AtomicInteger();

    public class TestObject {
        int id;
        boolean closed;

        @Override
        public String toString() {
            return "TestObject{" +
                    "id=" + id +
                    ", closed=" + closed +
                    '}';
        }
    }

    @Test
    public void test() {
        Pool.ObjectFactory<TestObject> factory = pool -> {
            CompletableFuture<PooledObject<TestObject>> future = new CompletableFuture<>();
            TestObject object = new TestObject();
            object.id = idGenerator.getAndIncrement();
            PooledObject<TestObject> pooledObject = new PooledObject<>(object, pool, obj -> System.out.println("leaked: " + obj.getObject()));
            future.complete(pooledObject);
            return future;
        };
        Pool.Validator<TestObject> validator = pooledObject -> !pooledObject.getObject().closed;
        Pool.Dispose<TestObject> dispose = pooledObject -> pooledObject.getObject().closed = true;


        int maxSize = 4;
        BoundObjectPool<TestObject> pool = new BoundObjectPool<>(maxSize, 4, 4,
                factory, validator, dispose,
                () -> System.out.println("no leak"));

        List<CompletableFuture<PooledObject<TestObject>>> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            list.add(pool.asyncGet());
        }

        list.forEach(future -> {
            try (PooledObject<TestObject> pooledObject = future.get()) {
                System.out.println(pooledObject);
                Assert.assertThat(pooledObject.getObject().closed, is(false));
                Assert.assertThat(pooledObject.getReleased().get(), is(false));
                Assert.assertTrue(pooledObject.getObject().id >= 0
                        && pooledObject.getObject().id < maxSize);

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        Assert.assertThat(pool.getCreatedCount().get(), is(maxSize));

        pool.stop();
    }
}
