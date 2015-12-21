package com.firefly.client.http2;

import java.nio.ByteBuffer;
import java.nio.channels.WritePendingException;
import java.util.concurrent.atomic.AtomicReference;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.stream.AbstractHTTP1Connection;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

public class HTTP1ClientConnection extends AbstractHTTP1Connection {

	private final ResponseHandlerWrap wrap;

	private static class ResponseHandlerWrap extends HTTPResponseHandler {

		private final AtomicReference<HTTPResponseHandler> writing = new AtomicReference<>();

		@Override
		public void earlyEOF() {
			writing.get().earlyEOF();
		}

		@Override
		public boolean content(ByteBuffer item, HTTPResponse response, HTTPConnection connection) {
			return writing.get().content(item, response, connection);
		}

		@Override
		public boolean headerComplete(HTTPResponse response, HTTPConnection connection) {
			return writing.get().headerComplete(response, connection);
		}

		@Override
		public boolean messageComplete(HTTPResponse response, HTTPConnection connection) {
			return writing.getAndSet(null).messageComplete(response, connection);
		}

		@Override
		public void badMessage(int status, String reason, HTTPResponse response, HTTPConnection connection) {
			writing.get().badMessage(status, reason, response, connection);
		}

	}

	public HTTP1ClientConnection(HTTP2Configuration config, SSLSession sslSession, Session tcpSession) {
		this(config, sslSession, tcpSession, new ResponseHandlerWrap());
	}

	private HTTP1ClientConnection(HTTP2Configuration config, SSLSession sslSession, Session tcpSession,
			ResponseHandler responseHandler) {
		super(config, sslSession, tcpSession, null, responseHandler);
		wrap = (ResponseHandlerWrap) responseHandler;
	}

	@Override
	protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler,
			ResponseHandler responseHandler) {
		return new HttpParser(responseHandler, config.getMaxRequestHeadLength());
	}

	HttpParser getParser() {
		return parser;
	}

	HttpGenerator getGenerator() {
		return generator;
	}

	public void request(HTTPRequest request, HTTPResponseHandler handler) {
		checkWrite(handler);
		// TODO
	}

	public void request(HTTPRequest request, ByteBuffer data, HTTPResponseHandler handler) {
		checkWrite(handler);
		// TODO
	}

	public void request(HTTPRequest request, ByteBuffer[] data, HTTPResponseHandler handler) {
		checkWrite(handler);
		// TODO
	}

	private void checkWrite(HTTPResponseHandler handler) {
		if (wrap.writing.compareAndSet(null, handler)) {
			handler.connection = this;
		} else {
			throw new WritePendingException();
		}
	}

}
