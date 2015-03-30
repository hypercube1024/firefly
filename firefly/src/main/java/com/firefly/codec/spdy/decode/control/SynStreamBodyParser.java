package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEvent;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.decode.utils.NumberProcessUtils;
import com.firefly.net.Session;

public class SynStreamBodyParser extends AbstractParser {

	public SynStreamBodyParser(SpdyDecodingEvent spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		if(!buffer.hasRemaining())
			return DecodeStatus.BUFFER_UNDERFLOW;
		
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		if(buffer.remaining() < attachment.controlFrameHeader.getLength()) {
			return DecodeStatus.BUFFER_UNDERFLOW;
		}
		
		// SYN stream basic fields length is 10 bytes 
		int streamId = NumberProcessUtils.toUnsigned31bitsInteger(buffer.getInt()); // 4 bytes
		int associatedStreamId = NumberProcessUtils.toUnsigned31bitsInteger(buffer.getInt()); // 4 bytes 
		byte b = buffer.get();  // 1 bytes
		int priority = b & 0b1110_0000;
		priority >>>= 5;
		byte slot = buffer.get(); // 1 bytes
		
		// TODO The header block length is the total length - 10 bytes
		return null;
	}

}
