package com.firefly.client.http2;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class HTTP1ClientResponseHandler implements ResponseHandler {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	protected HTTP1ClientConnection connection;
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
		try {
			return messageComplete(response, connection);
		} finally {
			HTTPClientRequest request = connection.getRequest();
			
			String requestConnectionValue = request.getFields().get(HttpHeader.CONNECTION);
			String responseConnectionValue = response.getFields().get(HttpHeader.CONNECTION);
			
			switch (response.getVersion()) {
			case HTTP_1_0:
				if("keep-alive".equalsIgnoreCase(requestConnectionValue) && "keep-alive".equalsIgnoreCase(responseConnectionValue)) {
					connection.reset();
				} else {
					connection.reset();
					try {
						connection.close();
					} catch (IOException e) {
						log.error("client closes connection exception", e);
					}
				}
				break;
			case HTTP_1_1: // the persistent connection is default in HTTP 1.1
				if("close".equalsIgnoreCase(requestConnectionValue) 
						|| "close".equalsIgnoreCase(responseConnectionValue)) {
					connection.reset();
					try {
						connection.close();
					} catch (IOException e) {
						log.error("client closes connection exception", e);
					}
				} else {
					connection.reset();
				}
				break;
			case HTTP_2:
				connection.reset();
				break;
			default:
				throw new IllegalStateException("client response does not support the http version " + connection.getHttpVersion());
			}
		}
	}

	@Override
	public final void badMessage(int status, String reason) {
		badMessage(status, reason, response, connection);
	}
	
	abstract public boolean content(ByteBuffer item, HTTPClientResponse response, HTTP1ClientConnection connection);
	
	abstract public boolean headerComplete(HTTPClientResponse response, HTTP1ClientConnection connection);
	
	abstract public boolean messageComplete(HTTPClientResponse response, HTTP1ClientConnection connection);
	
	abstract public void badMessage(int status, String reason, HTTPClientResponse response, HTTP1ClientConnection connection);
	
}
