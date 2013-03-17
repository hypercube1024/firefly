package test.net.ssl;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.net.support.ssl.SSLSession;

public class SSLDecoder implements Decoder {
	
	private DumpDecoder next = new DumpDecoder();

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {	
		SSLSession c = (SSLSession)session.getAttribute("_secure");

		ByteBuffer plaintext = c.read(buf);
		if(plaintext != null)
			next.decode(plaintext, session);
	}

}
