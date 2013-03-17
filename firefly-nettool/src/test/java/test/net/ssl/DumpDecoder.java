package test.net.ssl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class DumpDecoder implements Decoder {
	
	private File file = new File("/Users/qiupengtao/ssldump.txt");

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		System.out.println("receive text: " + buf.remaining());

		FileOutputStream out = new FileOutputStream(file, true);
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		out.write(b);
		out.close();
		
		session.fireReceiveMessage(null);
	}

}
