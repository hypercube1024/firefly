package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.core.AbstractLifeCycle;

public class HTTP2Client extends AbstractLifeCycle{
	
	private final HTTP2Configuration http2Configuration;

	public HTTP2Client(HTTP2Configuration http2Configuration) {
		this.http2Configuration = http2Configuration;
	}

	@Override
	public void start() {
		if(isStarted())
			return;
		
		synchronized(this) {
			if(isStarted())
				return;
			
			
			start = true;
		}
	}

	@Override
	public void stop() {
		if(isStopped())
			return;
		
		synchronized(this) {
			if(isStopped())
				return;
			// TODO implements the stop method
			
			start = false;
		}
	}

}
