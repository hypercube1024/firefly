package test.utils.lang.pool;

import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.lang.pool.BlockingPool;
import com.firefly.utils.lang.pool.BoundedBlockingPool;

public class TestBoundedBlockingPool {

	@Test
	public void test() throws InterruptedException {
		int maxSize = 10;
		AtomicInteger i = new AtomicInteger();
		BlockingPool<PooledObject> pool = new BoundedBlockingPool<PooledObject>(3, maxSize, () -> {
			int n = i.getAndIncrement();
			System.out.println("create obj - [" + n + "]");
			return new PooledObject(n);
		}, (o) -> {
			return !o.closed;
		}, (o) -> {
			System.out.println("destory obj - [" + o.i + "]");
			o.closed = true;
		});

		PooledObject o = pool.take();
		Assert.assertThat(pool.size(), is(2));
		pool.release(o);
		Assert.assertThat(pool.size(), is(3));
		System.out.println("-------------------------");
		List<PooledObject> list = new ArrayList<>();
		for (int j = 0; j < 13; j++) {
			list.add(pool.get());
		}

		list.stream().forEach(e -> {
			pool.release(e);
		});

		Assert.assertThat(list.get(10).closed, is(true));
		Assert.assertThat(list.get(9).closed, is(false));
		Assert.assertThat(pool.size(), is(10));
	}

	public static class PooledObject {
		public boolean closed = false;
		public int i;

		public PooledObject(int i) {
			this.i = i;
		}

	}

	public static void main(String[] args) {
		BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(2);
		for (int i = 0; i < 10; i++) {
			System.out.println(queue.offer(i));
		}
		System.out.println(queue.size());
		for (int i = 0; i < 10; i++) {
			System.out.println(queue.poll());
		}
	}
}
