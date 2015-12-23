package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.HTTPConnection;

public abstract class HTTPResponseHandler implements ResponseHandler {
	
	protected HTTPConnection connection;
	protected HTTPClientResponse response;

	@Override
	public final boolean startResponse(HttpVersion version, int status, String reason) {
		response = new HTTPClientResponse(version, status, reason);
		return false;
	}
	
	@Override
	public final void parsedHeader(HttpField field) {
		response.getFields().add(field);
	}
	
	@Override
	public final int getHeaderCacheSize() {
		return 1024;
	}

	@Override
	public final boolean content(ByteBuffer item) {
		return content(item, response, connection);
	}

	@Override
	public final boolean headerComplete() {
		return headerComplete(response, connection);
	}

	@Override
	public final boolean messageComplete() {
		return messageComplete(response, connection);
	}

	@Override
	public final void badMessage(int status, String reason) {
		badMessage(status, reason, response, connection);
	}
	
	abstract public boolean content(ByteBuffer item, HTTPClientResponse response, HTTPConnection connection);
	
	abstract public boolean headerComplete(HTTPClientResponse response, HTTPConnection connection);
	
	abstract public boolean messageComplete(HTTPClientResponse response, HTTPConnection connection);
	
	abstract public void badMessage(int status, String reason, HTTPClientResponse response, HTTPConnection connection);
	
}
