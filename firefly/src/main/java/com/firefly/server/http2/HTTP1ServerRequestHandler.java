package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

abstract public class HTTP1ServerRequestHandler implements RequestHandler {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	protected HTTPServerRequest request;
	protected HTTPServerResponse response;
	protected HTTP1ServerConnection connection;

	@Override
	public boolean startRequest(String method, String uri, HttpVersion version) {
		if (log.isDebugEnabled()) {
			log.debug("server received the request line, {}, {}, {}", method, uri, version);
		}
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
		String expectedValue = request.getFields().get(HttpHeader.EXPECT);
		if ("100-continue".equalsIgnoreCase(expectedValue)) {
			boolean skipNext = accept100Continue(request, response, connection);
			if (skipNext) {
				return true;
			} else {
				response.response100Continue();
				return headerComplete(request, response, connection);
			}
		} else {
			boolean success = connection.upgradeProtocolToHTTP2(request, response);
			if(success) {
				return true;
			} else {
				return headerComplete(request, response, connection);
			}
		}
	}

	@Override
	public boolean content(ByteBuffer item) {
		return content(item, request, response, connection);
	}

	@Override
	public boolean messageComplete() {
		try {
			return messageComplete(request, response, connection);
		} finally {
			connection.getParser().reset();
		}
	}

	@Override
	public void badMessage(int status, String reason) {
		badMessage(status, reason, request, response, connection);
	}

	@Override
	public void earlyEOF() {
		earlyEOF(request, response, connection);
	}

	@Override
	public int getHeaderCacheSize() {
		return 1024;
	}

	abstract public boolean accept100Continue(HTTPServerRequest request, HTTPServerResponse response,
			HTTP1ServerConnection connection);

	abstract public boolean content(ByteBuffer item, HTTPServerRequest request, HTTPServerResponse response,
			HTTP1ServerConnection connection);

	abstract public boolean headerComplete(HTTPServerRequest request, HTTPServerResponse response,
			HTTP1ServerConnection connection);

	abstract public boolean messageComplete(HTTPServerRequest request, HTTPServerResponse response,
			HTTP1ServerConnection connection);

	abstract public void badMessage(int status, String reason, HTTPServerRequest request, HTTPServerResponse response,
			HTTP1ServerConnection connection);

	abstract public void earlyEOF(HTTPServerRequest request, HTTPServerResponse response,
			HTTP1ServerConnection connection);

	public static class Adapter extends HTTP1ServerRequestHandler {

		@Override
		public boolean content(ByteBuffer item, HTTPServerRequest request, HTTPServerResponse response,
				HTTP1ServerConnection connection) {
			return false;
		}

		@Override
		public boolean headerComplete(HTTPServerRequest request, HTTPServerResponse response,
				HTTP1ServerConnection connection) {
			return false;
		}

		@Override
		public boolean messageComplete(HTTPServerRequest request, HTTPServerResponse response,
				HTTP1ServerConnection connection) {
			return true;
		}

		@Override
		public void badMessage(int status, String reason, HTTPServerRequest request, HTTPServerResponse response,
				HTTP1ServerConnection connection) {

		}

		@Override
		public void earlyEOF(HTTPServerRequest request, HTTPServerResponse response, HTTP1ServerConnection connection) {

		}

		@Override
		public boolean accept100Continue(HTTPServerRequest request, HTTPServerResponse response,
				HTTP1ServerConnection connection) {
			return false;
		}

	}

}
