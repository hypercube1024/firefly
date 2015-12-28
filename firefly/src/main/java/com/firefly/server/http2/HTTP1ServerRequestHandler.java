package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpVersion;

abstract public class HTTP1ServerRequestHandler implements RequestHandler {

	protected HTTPServerRequest request;
	protected HTTPServerResponse response;
	protected HTTP1ServerConnection connection;

	@Override
	public boolean startRequest(String method, String uri, HttpVersion version) {
		request = new HTTPServerRequest(method, uri, version);
		response = new HTTPServerResponse(request, connection);
		return false;
	}

	@Override
	public void parsedHeader(HttpField field) {
		request.getFields().add(field);
	}

	@Override
	public boolean headerComplete() {
		return headerComplete(request, response);
	}

	@Override
	public boolean content(ByteBuffer item) {
		return content(item, request, response);
	}

	@Override
	public boolean messageComplete() {
		try {
			return messageComplete(request, response);
		} finally {
			connection.getParser().reset();
		}
	}

	@Override
	public void badMessage(int status, String reason) {
		badMessage(status, reason, request, response);
	}

	@Override
	public void earlyEOF() {
		earlyEOF(request, response);
	}

	@Override
	public int getHeaderCacheSize() {
		return 1024;
	}

	abstract public boolean content(ByteBuffer item, HTTPServerRequest request, HTTPServerResponse response);

	abstract public boolean headerComplete(HTTPServerRequest request, HTTPServerResponse response);

	abstract public boolean messageComplete(HTTPServerRequest request, HTTPServerResponse response);

	abstract public void badMessage(int status, String reason, HTTPServerRequest request, HTTPServerResponse response);

	abstract public void earlyEOF(HTTPServerRequest request, HTTPServerResponse response);

	public static class Adapter extends HTTP1ServerRequestHandler {

		@Override
		public void earlyEOF(HTTPServerRequest request, HTTPServerResponse response) {
		}

		@Override
		public boolean content(ByteBuffer item, HTTPServerRequest request, HTTPServerResponse response) {
			return false;
		}

		@Override
		public boolean headerComplete(HTTPServerRequest request, HTTPServerResponse response) {
			return false;
		}

		@Override
		public boolean messageComplete(HTTPServerRequest request, HTTPServerResponse response) {
			return true;
		}

		@Override
		public void badMessage(int status, String reason, HTTPServerRequest request, HTTPServerResponse response) {
		}

	}

}
