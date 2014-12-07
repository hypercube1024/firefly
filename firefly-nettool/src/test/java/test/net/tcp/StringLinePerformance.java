package test.net.tcp;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import test.net.tcp.example.PipelineClientHandler;
import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

import com.firefly.net.support.wrap.client.MessageReceivedCallback;
import com.firefly.net.support.wrap.client.SimpleTcpClient;
import com.firefly.net.support.wrap.client.TcpConnection;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class StringLinePerformance {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	public static final int LOOP = 2000;
	public static final int THREAD = 500;

	public static class ClientSynchronizeTask implements Runnable {

		private final SimpleTcpClient client;
		private final CyclicBarrier barrier;

		@Override
		public void run() {
			TcpConnection c = null;
			try {
				c = client.connect().get();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
			try {
				for (int i = 0; i < LOOP; i++) {
					String message = "hello world! " + c.getId();
					String ret = (String) c.send(message).get();
					log.debug("rev: {}", ret);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
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
			TcpConnection c = null;
			try {
				c = client.connect().get();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
			for (int i = 0; i < LOOP; i++) {
				String message = "hello world! " + c.getId();
				c.send(message, new MessageReceivedCallback() {

					@Override
					public void messageRecieved(TcpConnection connection, Object obj) {
						log.debug("rev: {}", obj);
					}
				});

			}
			c.send("quit", new MessageReceivedCallback() {

				@Override
				public void messageRecieved(TcpConnection connection, Object obj) {
					log.debug("rev: {}", obj);
					log.debug("session {} complete", connection.getId());
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
				new StringLineDecoder(), new StringLineEncoder(), new PipelineClientHandler(), "nio");
		final CyclicBarrier barrier = new CyclicBarrier(THREAD, new StatTask());

		for (int i = 0; i < THREAD; i++)
			executorService.submit(new ClientSynchronizeTask(client, barrier));

//		for (int i = 0; i < THREAD; i++)
//			executorService.submit(new ClientAsynchronousTask(client, barrier));
	}
}
