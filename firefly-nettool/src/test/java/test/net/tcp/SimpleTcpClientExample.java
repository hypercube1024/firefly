package test.net.tcp;

import test.net.tcp.example.PipelineClientHandler;
import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

import com.firefly.net.support.wrap.client.MessageReceivedCallback;
import com.firefly.net.support.wrap.client.SimpleTcpClient;
import com.firefly.net.support.wrap.client.TcpConnection;
import com.firefly.utils.log.LogFactory;

public class SimpleTcpClientExample {
	public static void main(String[] args) throws Throwable {
		final SimpleTcpClient client = new SimpleTcpClient("localhost", 9900, new StringLineDecoder(), new StringLineEncoder(), new PipelineClientHandler());
		long start = System.currentTimeMillis();
		TcpConnection c = client.connect();
		long end = System.currentTimeMillis();
		System.out.println("connection 0 creating time is " + (end - start));
		System.out.println("current conn id: " + c.getId());
		c.send("hello client 1", new MessageReceivedCallback() {

			@Override
			public void messageRecieved(TcpConnection connection, Object obj) {
				System.out.println("con1|" + obj.toString());

			}
		});

		c.send("test client 2", new MessageReceivedCallback() {

			@Override
			public void messageRecieved(TcpConnection connection, Object obj) {
				System.out.println("con1|" + obj.toString());

			}
		});

		c.send("test client 3", new MessageReceivedCallback() {

			@Override
			public void messageRecieved(TcpConnection connection, Object obj) {
				System.out.println("con1|" + obj.toString());
			}
		});

		c.send("quit", new MessageReceivedCallback() {

			@Override
			public void messageRecieved(TcpConnection connection, Object obj) {
				System.out.println("con1|" + obj.toString());
			}
		});

		TcpConnection c2 = client.connect();
		System.out.println("con2|" + c2.send("getfile"));
		c2.close(false);
		
		TcpConnection c3 = client.connect();
		c3.send("test c3", new MessageReceivedCallback() {

			@Override
			public void messageRecieved(TcpConnection connection, Object obj) {
				System.out.println("con3|" + obj.toString());
			}
		});
		c3.close(true);;
		
		Thread.sleep(4000);
		client.shutdown();
		LogFactory.getInstance().shutdown();
//		System.out.println("shutdown");

		
	}
}
