package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.exception.DecodingStateException;
import com.firefly.net.Decoder;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SpdyDecoder implements Decoder {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final Parser parser;
	
	public SpdyDecoder(SpdyDecodingEvent spdyDecodingEvent) {
		parser = new SpdyParser(spdyDecodingEvent);
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		final ByteBuffer now = getBuffer(buf, session);
		DecodeStatus decodeStatus = parser.parse(now, session);
		log.debug("spdy decoding status: {}", decodeStatus);
		
		while(true) {
			switch (decodeStatus) {
			case INIT:
				reset(now, session);
				decodeStatus = parser.parse(now, session);
				break;
			case BUFFER_UNDERFLOW:
				save(now, session);
				return;
			case COMPLETE:
				reset(now, session);
				return;
			case ERROR:
				return;
			default:
				throw new DecodingStateException("Parsing SPDY frame decoding status error");
			}
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
	
	private void reset(ByteBuffer buf, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		attachment.byteBuffer = null;
		attachment.frameType = null;
		attachment.controlFrameParserState = ControlFrameParserState.HEAD;
		attachment.controlFrameHeader = null;
		// TODO reset
	}

}
