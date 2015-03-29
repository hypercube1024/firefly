package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.exception.DecodingStateException;
import com.firefly.net.Session;

public class SpdyParser implements Parser {
	
	private static final ControlFrameParser controlFrameParser = new ControlFrameParser();

	@Override
	public DecodeStatus parse(ByteBuffer buf, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		while(buf.hasRemaining()) {
			if(attachment.frameType == null) {
				// We must only peek the first byte and not advance the buffer
                // because the 7 least significant bits may be relevant in data frames
                int currByte = buf.get(buf.position());
                boolean isControlFrame = (currByte & 0x80) == 0x80;
                attachment.frameType = isControlFrame ? FrameType.CONTROL_FRAME : FrameType.DATA_FRAME;
                break;
			}
			switch (attachment.frameType) {
			case CONTROL_FRAME:
				DecodeStatus decodeStatus = controlFrameParser.parse(buf, session);
				if(decodeStatus == DecodeStatus.COMPLETE) {
					attachment.frameType = null;
					break;
				} else {
					return decodeStatus;
				}
			case DATA_FRAME:
				
				break;
			default:
				throw new DecodingStateException("Parsing SPDY frame type error");
			}
		}
		return DecodeStatus.COMPLETE;
	}

}
