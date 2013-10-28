package test.net.tcp;

import com.firefly.net.Config;
import com.firefly.net.Server;
import com.firefly.net.tcp.TcpServer;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

import org.junit.Assert;
import org.junit.Test;

import test.net.tcp.example.SimpleTcpClient;
import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;
import test.net.tcp.example.TcpConnection;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.is;

public class TestTcpClientAndServer {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@Test
	public void testHello() {
		Server server = new TcpServer();
		Config config = new Config();
		config.setDecoder(new StringLineDecoder());
		config.setEncoder(new StringLineEncoder());
		config.setHandler(new SendFileHandler());
		server.setConfig(config);
		server.start("localhost", 9900);

		final int LOOP = 50;
		ExecutorService executorService = Executors.newFixedThreadPool(LOOP);
		final SimpleTcpClient client = new SimpleTcpClient("localhost", 9900,
				new StringLineDecoder(), new StringLineEncoder());

		for (int i = 0; i < LOOP; i++) {
			executorService.submit(new Runnable() {
				@Override
				public void run() {
					final TcpConnection c = client.connect();
					Assert.assertThat(c.isOpen(), is(true));
					log.debug("main thread {}", Thread.currentThread()
							.toString());
					Assert.assertThat((String) c.send("hello client"), is("hello client"));
					Assert.assertThat((String) c.send("hello multithread test"), is("hello multithread test"));
					Assert.assertThat((String) c.send("getfile"), is("zero copy file transfers"));
					Assert.assertThat((String) c.send("quit"), is("bye!"));
					log.debug("complete session {}", c.getId());
				}
			});

		}

		final TcpConnection c = client.connect();
		Assert.assertThat((String) c.send("hello client 2"), is("hello client 2"));
		Assert.assertThat((String) c.send("quit"), is("bye!"));
	}
}
