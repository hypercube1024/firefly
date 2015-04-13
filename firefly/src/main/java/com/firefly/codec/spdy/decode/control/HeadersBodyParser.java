package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEventListener;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.utils.NumberProcessUtils;
import com.firefly.net.Session;

public class HeadersBodyParser extends AbstractParser {

	public HeadersBodyParser(SpdyDecodingEventListener spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		if(isControlFrameUnderflow(buffer, session))
			return DecodeStatus.BUFFER_UNDERFLOW;
		
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		
		int streamId = NumberProcessUtils.toUnsigned31bitsInteger(buffer.getInt()); // 4 bytes
		// The header block length is the total length - 4 bytes
		int headerBlockLength = attachment.controlFrameHeader.getLength() - 4;
		HeadersFrame headersFrame = null;
		if(headerBlockLength > 0) {
			// header block parser
			Fields fields = attachment.getConnection().getHeadersBlockParser().parse(streamId, headerBlockLength, buffer, session);
			headersFrame = new HeadersFrame(attachment.controlFrameHeader.getVersion(),
					attachment.controlFrameHeader.getFlags(), 
					streamId, fields);
		} else {
			headersFrame = new HeadersFrame(attachment.controlFrameHeader.getVersion(),
					attachment.controlFrameHeader.getFlags(), 
					streamId, null);
		}
		spdyDecodingEvent.onHeaders(headersFrame, session);
		return buffer.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
	}

}
