package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEventListener;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.net.Session;
import com.firefly.utils.codec.NumberProcessUtils;

public class SynReplyBodyParser extends AbstractParser {

	public SynReplyBodyParser(SpdyDecodingEventListener spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		if(isControlFrameUnderflow(buffer, session))
			return DecodeStatus.BUFFER_UNDERFLOW;
		
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		
		// SYN replay basic field length is 4 bytes
		int streamId = NumberProcessUtils.toUnsigned31bitsInteger(buffer.getInt()); // 4 bytes
		// The header block length is the total length - 4 bytes
		int headerBlockLength = attachment.controlFrameHeader.getLength() - 4;
		log.debug("The syn replay header block's length is {}", headerBlockLength);
		
		SynReplyFrame synReplyFrame = null;
		if(headerBlockLength > 0) {
			// header block parser
			Fields fields = attachment.getConnection().getHeadersBlockParser().parse(streamId, headerBlockLength, buffer, session);
			synReplyFrame = new SynReplyFrame(attachment.controlFrameHeader.getVersion(),
					attachment.controlFrameHeader.getFlags(), 
					streamId, fields);
		} else {
			synReplyFrame = new SynReplyFrame(attachment.controlFrameHeader.getVersion(),
					attachment.controlFrameHeader.getFlags(), 
					streamId, null);
		}
		spdyDecodingEvent.onSynReply(synReplyFrame, session);
		return buffer.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
	}

}
