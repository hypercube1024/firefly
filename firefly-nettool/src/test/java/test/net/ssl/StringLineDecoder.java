package test.net.ssl;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class StringLineDecoder implements Decoder {
	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private static final byte LINE_LIMITOR = '\n';

	@Override
	public void decode(ByteBuffer buffer, Session session) throws Throwable {
//		log.info("into decode, {}", buffer.remaining());
		SessionInfo sessionInfo = (SessionInfo)session.getAttachment();
		ByteBuffer now = buffer;
		ByteBuffer prev = sessionInfo.byteBuffer;

		if (prev != null) {
			sessionInfo.byteBuffer = null;
			now = (ByteBuffer) ByteBuffer
					.allocate(prev.remaining() + buffer.remaining()).put(prev)
					.put(buffer).flip();
		}

		int dataLen = now.position() + now.remaining();

		for (int i = now.position(), p = i; i < dataLen; i++) {
			if (now.get(i) == LINE_LIMITOR) {
				byte[] data = new byte[i - p + 1];
				now.get(data);
				String line = new String(data);
				p = i + 1;
//				log.debug("current str: {}|{}|{}|{}", line, line.length(), (int)line.charAt(0), LINE_LIMITOR);
				session.fireReceiveMessage(line.trim());
			}
		}

		if (now.hasRemaining())
			sessionInfo.byteBuffer = now;
	}
	
	public static void main(String[] args) {
		ByteBuffer buf = ByteBuffer.allocate(16);
		buf.putInt(1);
		buf.putInt(2);
		buf.putInt(3);
		buf.putInt(4);
		buf.flip();
		System.out.println(buf.getInt() + "|" + buf.getInt() + "\t" + buf.position() + "|" + buf.remaining());
		ByteBuffer buf2 = buf.slice();
		System.out.println(buf2.position() + "|" + buf2.remaining());
		System.out.println(buf2.getInt());
		System.out.println(buf2.getInt());
	}

}
