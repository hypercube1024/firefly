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
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		log.debug("control frame's length is {}", attachment.controlFrameHeader.getLength());
		return buffer.remaining() < attachment.controlFrameHeader.getLength();
	}
	
	protected boolean isDataFrameUnderflow(ByteBuffer buffer, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		log.debug("data frame's length is {}", attachment.dataFrame.getLength());
		return buffer.remaining() < attachment.dataFrame.getLength();
	}

}
