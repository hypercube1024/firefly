package com.firefly.server.http2.servlet;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.ServerHTTPHandler;

public class ServletServerHTTPHandler extends ServerHTTPHandler.Adapter {

	protected final HTTP2Configuration http2Configuration;

	public ServletServerHTTPHandler(HTTP2Configuration http2Configuration) {
		this.http2Configuration = http2Configuration;
	}

	@Override
	public boolean headerComplete(Request request, Response response, HTTPOutputStream output,
			HTTPConnection connection) {
		HTTPServletRequestImpl servletRequest = new HTTPServletRequestImpl(http2Configuration, request, connection);
		request.setAttachment(servletRequest);
		return false;
	}

	@Override
	public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
			HTTPConnection connection) {
		return false;
	}

	@Override
	public boolean messageComplete(Request request, Response response, HTTPOutputStream output,
			HTTPConnection connection) {
		return true;
	}

}
