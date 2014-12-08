package test.net.ssl;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

public class SSLDecoder implements Decoder {
	
//	private DumpDecoder next = new DumpDecoder();
	private StringLineDecoder next = new StringLineDecoder();

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		SessionInfo info = (SessionInfo)session.getAttachment();
		SSLSession c = info.sslSession;

		ByteBuffer plaintext = c.read(buf);
		if(plaintext != null)
			next.decode(plaintext, session);
	}

}
