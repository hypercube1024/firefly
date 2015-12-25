package test.net.tcp;

import static org.hamcrest.Matchers.is;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.net.Config;
import com.firefly.net.Server;
import com.firefly.net.support.wrap.client.SimpleTcpClient;
import com.firefly.net.support.wrap.client.TcpConnection;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

import test.net.tcp.example.PipelineClientHandler;
import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

public class TestTcpClientAndServer {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@Test
	public void testHello() throws Throwable {
		Server server = new AsynchronousTcpServer();
		Config config = new Config();
		config.setDecoder(new StringLineDecoder());
		config.setEncoder(new StringLineEncoder());
		config.setHandler(new SendFileHandler());
		server.setConfig(config);
		server.start("localhost", 9900);
		Thread.sleep(1000);
		
		final int LOOP = 50;
		final CountDownLatch requestLatch = new CountDownLatch(LOOP);
		ExecutorService executorService = Executors.newFixedThreadPool(LOOP);
		final SimpleTcpClient client = new SimpleTcpClient("localhost", 9900, new StringLineDecoder(), new StringLineEncoder(), new PipelineClientHandler());

		for (int i = 0; i < LOOP; i++) {
			executorService.submit(new Runnable() {
				@Override
				public void run() {
					TcpConnection c = null;
					try {
						c = client.connect().get();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					Assert.assertThat(c.isOpen(), is(true));
					log.debug("main thread {}", Thread.currentThread()
							.toString());
					try {
						Assert.assertThat((String) c.send("hello client").get(), is("hello client"));
						Assert.assertThat((String) c.send("hello multithread test").get(), is("hello multithread test"));
						Assert.assertThat((String) c.send("getfile").get(), is("zero copy file transfers"));
						Assert.assertThat((String) c.send("quit").get(), is("bye!"));
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					
					log.debug("complete session {}", c.getId());
					requestLatch.countDown();
				}
			});

		}

		requestLatch.await();
		TcpConnection c = client.connect().get();
		try {
			Assert.assertThat((String) c.send("hello client 2").get(), is("hello client 2"));
			Assert.assertThat((String) c.send("quit").get(), is("bye!"));
		} finally {
			c.close();
		}
		
	}
}
