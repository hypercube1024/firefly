package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEvent;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.decode.utils.NumberProcessUtils;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
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
		
		log.debug("control frame's length is {}", attachment.controlFrameHeader.getLength());
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
		
		// The header block length is the total length - 10 bytes
		int headerBlockLength = attachment.controlFrameHeader.getLength() - 10;
		log.debug("The syn stream header block's length is {}", headerBlockLength);
		
		SynStreamFrame synStreamFrame = null;
		if(headerBlockLength > 0) {
			// TODO header block parser
			Fields fields = HeadersBlockParser.DEFAULT_PARSER.parse(associatedStreamId, headerBlockLength, buffer, session);
			synStreamFrame = new SynStreamFrame(attachment.controlFrameHeader.getVersion(), 
					attachment.controlFrameHeader.getFlags(), 
					streamId, associatedStreamId, 
					(byte)priority, slot, fields);
		} else {
			synStreamFrame = new SynStreamFrame(attachment.controlFrameHeader.getVersion(), 
					attachment.controlFrameHeader.getFlags(), 
					streamId, associatedStreamId, 
					(byte)priority, slot, null);
		}
		spdyDecodingEvent.onSynStream(synStreamFrame, session);
		return buffer.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
	}

}
