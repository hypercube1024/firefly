package test.utils.log;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

import java.util.concurrent.Phaser;

public class LogBenchmark {

	private static final Log log = LogFactory.getInstance().getLog("test-INFO");

	public static void main(String[] args) {
		test(4, 5_000_000, 20);
	}

	public static void test(int threadNum, int messageNum, int messageSize) {
		StringBuilder data = new StringBuilder(messageSize);
		for (int i = 0; i < messageSize; i++) {
			data.append("a");
		}
		String str = data.toString();

		final Phaser phaser = new Phaser(threadNum + 1);

		Thread[] threads = new Thread[threadNum];
		int size = messageNum / threadNum;
		System.out.println("size: " + size);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				for (int j = 0; j < size; j++) {
					log.info(str);
				}
//				System.out.println(Thread.currentThread().getName() + " arrived");
				phaser.arrive();
			} , "test-thread-" + i);
		}

		long start = System.currentTimeMillis();
		for (int i = 0; i < threads.length; i++) {
			threads[i].start();
		}
		phaser.arriveAndAwaitAdvance();
		long end = System.currentTimeMillis();
		long time = (end - start) / 1000;
		System.out.println("time: " + time);
		System.out.println("msg/sec: " + (messageNum / time));
	}

}
