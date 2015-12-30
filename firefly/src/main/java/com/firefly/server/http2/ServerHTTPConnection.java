package com.firefly.server.http2;

import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPConnection;

public interface ServerHTTPConnection extends HTTPConnection {

	public MetaData.Request getRequest();
	
	public MetaData.Response getResponse();
	
	public void response100Continue();
}
