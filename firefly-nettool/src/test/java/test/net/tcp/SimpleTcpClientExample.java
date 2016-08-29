package test.net.tcp;

import java.util.concurrent.Future;

import test.net.tcp.example.PipelineClientHandler;
import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

import com.firefly.net.support.wrap.client.MessageReceivedCallback;
import com.firefly.net.support.wrap.client.SimpleTcpClient;
import com.firefly.net.support.wrap.client.TcpConnection;

public class SimpleTcpClientExample {
	public static void main(String[] args) throws Throwable {
		final SimpleTcpClient client = new SimpleTcpClient("localhost", 9900, new StringLineDecoder(), new StringLineEncoder(), new PipelineClientHandler());
		long start = System.currentTimeMillis();
		TcpConnection c = client.connect().get();
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

		Future<TcpConnection> fc2 = client.connect();
		Future<TcpConnection> fc3 = client.connect();
		
		
		TcpConnection c2 = fc2.get();
		TcpConnection c3 = fc3.get();
		System.out.println("con2|" + c2.send("getfile").get());
		c2.close();
		
		c3.send("test c3", new MessageReceivedCallback() {

			@Override
			public void messageRecieved(TcpConnection connection, Object obj) {
				System.out.println("con3|" + obj.toString());
			}
		});
		c3.close();;
		
		Thread.sleep(4000);
		client.shutdown();
//		System.out.println("shutdown");

		
	}
}
