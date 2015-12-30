package com.firefly.server.http2;

import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;

public class HTTPServerResponse extends MetaData.Response {

	public HTTPServerResponse() {
		super(HttpVersion.HTTP_1_1, 0, new HttpFields());
	}

	public HTTPServerResponse(int status, HttpFields fields, long contentLength) {
		super(HttpVersion.HTTP_1_1, status, fields, contentLength);
	}

	public HTTPServerResponse(int status, String reason, HttpFields fields, long contentLength) {
		super(HttpVersion.HTTP_1_1, status, reason, fields, contentLength);
	}

}
