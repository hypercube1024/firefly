package test.net.ssl;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.firefly.net.Encoder;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

public class SSLEncoder implements Encoder {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		SessionInfo info = (SessionInfo)session.getAttachment();
		SSLSession c = info.sslSession;
		
		if(message instanceof String) {
			byte[] body = ((String)message).getBytes("UTF-8");
			byte[] head = ("HTTP/1.1 200 OK\r\n"
					+ "Server: firefly-server/1.0\r\n"
					+ "Content-Length: " + body.length + "\r\n"
					+ "\r\n")
				.getBytes("UTF-8");
		
			c.write(ByteBuffer.wrap(head));
			c.write(ByteBuffer.wrap(body));
		} else if (message instanceof FileChannel) {
			Long length = info.length;
			byte[] head = ("HTTP/1.1 200 OK\r\n"
					+ "Server: firefly-server/1.0\r\n"
					+ "Content-Length: " + length + "\r\n"
					+ "\r\n")
				.getBytes("UTF-8");
			
			c.write(ByteBuffer.wrap(head));
			c.transferTo((FileChannel)message, 0, length);
		}
	}

}
