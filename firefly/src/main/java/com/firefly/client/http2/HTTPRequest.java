package com.firefly.client.http2;

import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;

public class HTTPRequest extends MetaData.Request {

	public HTTPRequest(String method, String uri) {
		super(method, new HttpURI(uri), HttpVersion.HTTP_1_1, new HttpFields(), -1);
	}

	public HTTPRequest(String method, String uri, int contentLength) {
		super(method, new HttpURI(uri), HttpVersion.HTTP_1_1, new HttpFields(), contentLength);
	}
	
	public HTTPRequest(String method, HttpURI uri, HttpFields fields, long contentLength) {
		super(method, uri, HttpVersion.HTTP_1_1, new HttpFields(), contentLength);
	}
	
	public HTTPRequest(String method, HttpURI uri, HttpVersion version, HttpFields fields, long contentLength) {
		super(method, uri, version, new HttpFields(), contentLength);
	}

	public HTTPRequest(MetaData.Request request) {
		super(request);
	}
}
