package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;


public abstract class AbstractParser implements Parser {

	protected static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	protected SpdyDecodingEvent spdyDecodingEvent;

	public AbstractParser(SpdyDecodingEvent spdyDecodingEvent) {
		this.spdyDecodingEvent = spdyDecodingEvent;
	}
	
	protected boolean isControlFrameUnderflow(ByteBuffer buffer, Session session) {
		if(!buffer.hasRemaining())
			return true;
		
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		log.debug("control frame's length is {}", attachment.controlFrameHeader.getLength());
		return buffer.remaining() < attachment.controlFrameHeader.getLength();
	}

}
