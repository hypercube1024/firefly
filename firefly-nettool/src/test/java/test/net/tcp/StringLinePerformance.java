package test.net.tcp;

import com.firefly.net.Session;
import com.firefly.net.support.StringLineDecoder;
import com.firefly.net.support.StringLineEncoder;
import com.firefly.net.support.TcpConnection;
import com.firefly.net.support.MessageReceiveCallBack;
import com.firefly.net.support.SimpleTcpClient;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import java.util.concurrent.*;

public class StringLinePerformance {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	public static final int LOOP = 2000;
	public static final int THREAD = 500;

	public static class ClientSynchronizeTask implements Runnable {

		private final SimpleTcpClient client;
		private final CyclicBarrier barrier;

		@Override
		public void run() {
			TcpConnection c = client.connect();
			try {
				for (int i = 0; i < LOOP; i++) {
					String message = "hello world! " + c.getId();
					String ret = (String) c.send(message);
					log.debug("rev: {}", ret);
				}
			} finally {
				if (c != null)
					c.close(false);
			}
			log.debug("session {} complete", c.getId());

			try {
				barrier.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			}

		}

		public ClientSynchronizeTask(SimpleTcpClient client,
				CyclicBarrier barrier) {
			this.client = client;
			this.barrier = barrier;
		}
	}

	public static class ClientAsynchronousTask implements Runnable {

		private final SimpleTcpClient client;
		private final CyclicBarrier barrier;

		@Override
		public void run() {
			TcpConnection c = client.connect();
			for (int i = 0; i < LOOP; i++) {
				String message = "hello world! " + c.getId();
				c.send(message, new MessageReceiveCallBack() {

					@Override
					public void messageRecieved(Session session, Object obj) {
						log.debug("rev: {}", obj);
					}
				});

			}
			c.send("quit", new MessageReceiveCallBack() {

				@Override
				public void messageRecieved(Session session, Object obj) {
					log.debug("rev: {}", obj);
					log.debug("session {} complete", session.getSessionId());
				}
			});
			try {
				barrier.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			}

		}

		public ClientAsynchronousTask(SimpleTcpClient client,
				CyclicBarrier barrier) {
			this.client = client;
			this.barrier = barrier;
		}
	}

	public static class StatTask implements Runnable {

		private long start;

		public StatTask() {
			this.start = System.currentTimeMillis();
		}

		@Override
		public void run() {
			long time = System.currentTimeMillis() - start;
			log.debug("start time: {}", start);
			log.debug("total time: {}", time);
			int reqs = LOOP * THREAD;

			double throughput = (reqs / (double) time) * 1000;
			log.info("throughput: {}", throughput);
		}

	}

	public static void main(String[] args) {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD);
		final SimpleTcpClient client = new SimpleTcpClient("localhost", 9900,
				new StringLineDecoder(), new StringLineEncoder());
		final CyclicBarrier barrier = new CyclicBarrier(THREAD, new StatTask());

		for (int i = 0; i < THREAD; i++)
			executorService.submit(new ClientSynchronizeTask(client, barrier));

//		for (int i = 0; i < THREAD; i++)
//			executorService.submit(new ClientAsynchronousTask(client, barrier));
	}
}
