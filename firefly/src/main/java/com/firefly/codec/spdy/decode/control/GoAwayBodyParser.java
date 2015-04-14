package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEventListener;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.SessionStatus;
import com.firefly.net.Session;
import com.firefly.utils.codec.NumberProcessUtils;

public class GoAwayBodyParser extends AbstractParser {

	public GoAwayBodyParser(SpdyDecodingEventListener spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		if(isControlFrameUnderflow(buffer, session))
			return DecodeStatus.BUFFER_UNDERFLOW;
		
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		int lastStreamId = NumberProcessUtils.toUnsigned31bitsInteger(buffer.getInt());
		SessionStatus sessionStatus = SessionStatus.from(buffer.getInt());
		GoAwayFrame goAwayFrame = new GoAwayFrame(attachment.controlFrameHeader.getVersion(), lastStreamId, sessionStatus);
		spdyDecodingEvent.onGoAway(goAwayFrame, session);
		return buffer.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
	}

}
