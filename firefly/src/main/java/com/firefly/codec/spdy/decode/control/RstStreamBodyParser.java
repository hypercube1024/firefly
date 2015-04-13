package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEventListener;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame.StreamErrorCode;
import com.firefly.codec.utils.NumberProcessUtils;
import com.firefly.net.Session;

public class RstStreamBodyParser extends AbstractParser {

	public RstStreamBodyParser(SpdyDecodingEventListener spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		if(isControlFrameUnderflow(buffer, session))
			return DecodeStatus.BUFFER_UNDERFLOW;
		
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		int streamId = NumberProcessUtils.toUnsigned31bitsInteger(buffer.getInt()); // 4 bytes
		int statusCode = buffer.getInt();
		RstStreamFrame rstStreamFrame = new RstStreamFrame(attachment.controlFrameHeader.getVersion(), 
				streamId, StreamErrorCode.from(statusCode));
		spdyDecodingEvent.onRstStream(rstStreamFrame, session);
		return buffer.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
	}

}
