package test.net.ssl;

import java.nio.ByteBuffer;

import com.firefly.net.Encoder;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Callback;

public class SSLEncoder implements Encoder {

	private static final String LINE_LIMITOR = System.getProperty("line.separator");
	
	@Override
	public void encode(Object message, Session session) throws Throwable {
		SessionInfo info = (SessionInfo)session.getAttachment();
		SSLSession c = info.sslSession;
		
		String str = message + LINE_LIMITOR;
		ByteBuffer byteBuffer = ByteBuffer.wrap(str.getBytes("UTF-8"));
		c.write(byteBuffer, Callback.NOOP);
	}

}
