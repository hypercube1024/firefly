package test.net.ssl;

import java.nio.ByteBuffer;

import com.firefly.net.Encoder;
import com.firefly.net.Session;
import com.firefly.net.support.ssl.SSLSession;

public class SSLEncoder implements Encoder {

	@Override
	public void encode(Object message, Session session) throws Throwable {
		SSLSession c = (SSLSession)session.getAttribute("_secure");
		byte[] body = ((String)message).getBytes("UTF-8");
		byte[] head = ("HTTP/1.1 200 OK\r\n"
					+ "Server: firefly-server/1.0\r\n"
					+ "Content-Length: " + body.length + "\r\n"
					+ "\r\n")
				.getBytes("UTF-8");
		
		c.write(ByteBuffer.wrap(head));
		c.write(ByteBuffer.wrap(body));
	}

}
