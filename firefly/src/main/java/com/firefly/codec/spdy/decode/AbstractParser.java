package com.firefly.codec.spdy.decode;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;


public abstract class AbstractParser implements Parser {

	protected static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	protected SpdyDecodingEvent spdyDecodingEvent;

	public AbstractParser(SpdyDecodingEvent spdyDecodingEvent) {
		this.spdyDecodingEvent = spdyDecodingEvent;
	}

}
