package com.firefly.server.http2;

import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;

public class HTTPServerRequest extends MetaData.Request {
	
	public HTTPServerRequest(String method, String uri, HttpVersion version) {
		super(method, new HttpURI(uri), version, new HttpFields());
	}
}
