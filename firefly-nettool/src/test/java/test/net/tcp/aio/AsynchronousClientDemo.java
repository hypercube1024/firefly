package test.net.tcp.aio;

import java.util.concurrent.Future;

import test.net.tcp.example.PipelineClientHandler;
import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

import com.firefly.net.support.wrap.client.MessageReceivedCallback;
import com.firefly.net.support.wrap.client.SimpleTcpClient;
import com.firefly.net.support.wrap.client.TcpConnection;

public class AsynchronousClientDemo {

	public static void main(String[] args) throws Throwable {
		final SimpleTcpClient client = new SimpleTcpClient(
				"localhost", 9900, 
				new StringLineDecoder(), 
				new StringLineEncoder(), 
				new PipelineClientHandler());
		
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
				System.out.println("con1 is open " + connection.isOpen());
			}
		});

		Future<TcpConnection> fc2 = client.connect();
		Future<TcpConnection> fc3 = client.connect();
		
		TcpConnection c2 = fc2.get();
		TcpConnection c3 = fc3.get();
		System.out.println("con2|" + c2.send("getfile").get());
		System.out.println("con3|" + c3.send("test c3").get());
		c2.close(false);
		c3.close(false);
		
		TcpConnection c4 = client.connect().get();
		for(;;) {
			c4.send("loop", new MessageReceivedCallback() {

				@Override
				public void messageRecieved(TcpConnection connection, Object obj) {
					System.out.println("con4|" + obj.toString());
				}
			});
			Thread.sleep(1000);
		}
	}

}
