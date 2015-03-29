package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.exception.DecodingStateException;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SpdyParser implements Parser {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private static final ControlFrameParser controlFrameParser = new ControlFrameParser();
	private static final DataFrameParser dataFrameParser = new DataFrameParser();

	@Override
	public DecodeStatus parse(ByteBuffer buf, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		
		log.debug("spdy decoding, current buffer {} bytes remaining", buf.remaining());
		while(buf.hasRemaining()) {
			if(attachment.frameType == null) {
				// We must only peek the first byte and not advance the buffer
                // because the 7 least significant bits may be relevant in data frames
                int currByte = buf.get(buf.position());
                boolean isControlFrame = (currByte & 0x80) == 0x80;
                attachment.frameType = isControlFrame ? FrameType.CONTROL_FRAME : FrameType.DATA_FRAME;
			}
			switch (attachment.frameType) {
			case CONTROL_FRAME:
				return controlFrameParser.parse(buf, session);
			case DATA_FRAME:
				return dataFrameParser.parse(buf, session);
			default:
				throw new DecodingStateException("Parsing SPDY frame type error");
			}
		}
		return DecodeStatus.BUFFER_UNDERFLOW;
	}

}
