package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.server.http2.HTTP1ServerConnection.HTTP1ServerResponseOutputStream;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP1ServerRequestHandler implements RequestHandler {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	protected MetaData.Request request;
	protected MetaData.Response response;
	protected HTTP1ServerConnection connection;
	protected HTTP1ServerResponseOutputStream outputStream;
	protected final ServerHTTPHandler serverHTTPHandler;
	
	HTTP1ServerRequestHandler(ServerHTTPHandler serverHTTPHandler) {
		this.serverHTTPHandler = serverHTTPHandler;
	}

	@Override
	public boolean startRequest(String method, String uri, HttpVersion version) {
		if (log.isDebugEnabled()) {
			log.debug("server received the request line, {}, {}, {}", method, uri, version);
		}
		
		request = new HTTPServerRequest(method, uri, version);
		response = new HTTPServerResponse();
		outputStream = new HTTP1ServerResponseOutputStream(response, connection);
		
		if(HttpMethod.PRI.is(method)) {
			return connection.upgradeProtocolToHTTP2(request, response);
		} else {
			return false;
		}
	}

	@Override
	public void parsedHeader(HttpField field) {
		request.getFields().add(field);
	}

	@Override
	public boolean headerComplete() {
		String expectedValue = request.getFields().get(HttpHeader.EXPECT);
		if ("100-continue".equalsIgnoreCase(expectedValue)) {
			boolean skipNext = serverHTTPHandler.accept100Continue(request, response, outputStream, connection);
			if (skipNext) {
				return true;
			} else {
				connection.response100Continue();
				return serverHTTPHandler.headerComplete(request, response, outputStream, connection);
			}
		} else {
			boolean success = connection.upgradeProtocolToHTTP2(request, response);
			if(success) {
				return true;
			} else {
				return serverHTTPHandler.headerComplete(request, response, outputStream, connection);
			}
		}
	}

	@Override
	public boolean content(ByteBuffer item) {
		return serverHTTPHandler.content(item, request, response, outputStream, connection);
	}

	@Override
	public boolean messageComplete() {
		try {
			if(connection.upgradeHTTP2Successfully) {
				return true;
			} else {
				return serverHTTPHandler.messageComplete(request, response, outputStream, connection);
			}
		} finally {
			connection.getParser().reset();
		}
	}

	@Override
	public void badMessage(int status, String reason) {
		serverHTTPHandler.badMessage(status, reason, request, response, outputStream, connection);
	}

	@Override
	public void earlyEOF() {
		serverHTTPHandler.earlyEOF(request, response, outputStream, connection);
	}

	@Override
	public int getHeaderCacheSize() {
		return 1024;
	}

}
