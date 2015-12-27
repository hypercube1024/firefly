package com.firefly.server.http2;

import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;

public class HTTPServerResponse extends MetaData.Response {
	
	public HTTPServerResponse() {
		this(0);
	}

	public HTTPServerResponse(int status) {
		super(HttpVersion.HTTP_1_1, status, new HttpFields());
	}

	public HTTPServerResponse(int status, long contentLength) {
		super(HttpVersion.HTTP_1_1, status, new HttpFields(), contentLength);
	}

	public HTTPServerResponse(int status, String reason, HttpFields fields, long contentLength) {
		super(HttpVersion.HTTP_1_1, status, reason, fields, contentLength);
	}
}
