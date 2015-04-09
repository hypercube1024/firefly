package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEventListener;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.decode.utils.NumberProcessUtils;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.net.Session;

public class WindowUpdateBodyParser extends AbstractParser {

	public WindowUpdateBodyParser(SpdyDecodingEventListener spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		if(isControlFrameUnderflow(buffer, session))
			return DecodeStatus.BUFFER_UNDERFLOW;
		
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		int streamId = NumberProcessUtils.toUnsigned31bitsInteger(buffer.getInt());
		int windowDelta = NumberProcessUtils.toUnsigned31bitsInteger(buffer.getInt());
		WindowUpdateFrame windowUpdateFrame = new WindowUpdateFrame(attachment.controlFrameHeader.getVersion(), 
				streamId, windowDelta);
		spdyDecodingEvent.onWindowUpdate(windowUpdateFrame, session);
		return buffer.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
	}

}
