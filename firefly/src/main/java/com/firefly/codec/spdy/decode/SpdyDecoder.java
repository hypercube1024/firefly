package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.net.Decoder;
import com.firefly.net.Session;

public class SpdyDecoder implements Decoder {
	
	private static final Parser parser = new SpdyParser();

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		final ByteBuffer now = getBuffer(buf, session);
		DecodeStatus decodeStatus = parser.parse(buf, session);
		switch (decodeStatus) {
		case BUFFER_UNDERFLOW:
			save(now, session);
			break;
		case COMPLETE:
			break;
		case ERROR:
			break;
		default:
			break;
		}
		
	}

	private ByteBuffer getBuffer(ByteBuffer buf, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		final ByteBuffer prev = attachment.byteBuffer;
		ByteBuffer now = buf;
		
		if (prev != null) {
			attachment.byteBuffer = null;
			now = (ByteBuffer) ByteBuffer
					.allocate(prev.remaining() + buf.remaining()).put(prev)
					.put(buf).flip();
		}
		return now;
	}
	
	private void save(ByteBuffer buf, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		if (buf.hasRemaining())
			attachment.byteBuffer = buf;
	}

}
