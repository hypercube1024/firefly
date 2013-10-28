package test.net.tcp.example;

import java.nio.ByteBuffer;

import com.firefly.net.Encoder;
import com.firefly.net.Session;

public class StringLineEncoder implements Encoder {

	private static final String LINE_LIMITOR = System.getProperty("line.separator");
	
	@Override
	public void encode(Object message, Session session) throws Throwable {
		String str = message + LINE_LIMITOR;

		ByteBuffer byteBuffer = ByteBuffer.wrap(str.getBytes());
		session.write(byteBuffer);
	}

}
