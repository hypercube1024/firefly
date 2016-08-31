package test.utils.lang.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestBoundedBlockingPool {

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
