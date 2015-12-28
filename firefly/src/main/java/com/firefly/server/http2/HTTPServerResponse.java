package com.firefly.server.http2;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.AbstractHTTP1OutputStream;
import com.firefly.net.Session;
import com.firefly.utils.io.BufferUtils;

public class HTTPServerResponse extends MetaData.Response {

	protected final HTTP1ServerResponseOutputStream outputStream;
	protected final HTTPServerRequest request;

	public HTTPServerResponse(HTTPServerRequest request, HTTP1ServerConnection connection) {
		super(HttpVersion.HTTP_1_1, 0, new HttpFields());
		outputStream = new HTTP1ServerResponseOutputStream(this, connection);
		this.request = request;
	}

	public HTTP1ServerResponseOutputStream getOutputStream() {
		if (getStatus() <= 0)
			throw new IllegalStateException("the server must set a response status before it outputs data");

		return outputStream;
	}

	public static class HTTP1ServerResponseOutputStream extends AbstractHTTP1OutputStream {

		private final HTTP1ServerConnection connection;

		public HTTP1ServerResponseOutputStream(HTTPServerResponse response, HTTP1ServerConnection connection) {
			super(response, false);
			this.connection = connection;
		}

		HTTP1ServerConnection getHTTP1ServerConnection() {
			return connection;
		}

		@Override
		protected void generateHTTPMessageSuccessfully() {
			log.debug("server session {} generates the HTTP message completely",
					connection.getTcpSession().getSessionId());

			final HTTPServerResponse response = (HTTPServerResponse) info;
			final HTTPServerRequest request = response.request;

			String requestConnectionValue = request.getFields().get(HttpHeader.CONNECTION);
			String responseConnectionValue = response.getFields().get(HttpHeader.CONNECTION);

			switch (request.getVersion()) {
			case HTTP_1_0:
				if ("keep-alive".equalsIgnoreCase(requestConnectionValue)
						&& "keep-alive".equalsIgnoreCase(responseConnectionValue)) {
					log.debug("the server {} connection is persistent", response.getVersion());
					connection.getGenerator().reset();
				} else {
					connection.getGenerator().reset();
					try {
						connection.close();
					} catch (IOException e) {
						log.error("server closes connection exception", e);
					}
				}
				break;
			case HTTP_1_1: // the persistent connection is default in HTTP 1.1
				if ("close".equalsIgnoreCase(requestConnectionValue)
						|| "close".equalsIgnoreCase(responseConnectionValue)) {
					log.debug("the server {} connection is persistent", response.getVersion());
					connection.getGenerator().reset();
					try {
						connection.close();
					} catch (IOException e) {
						log.error("server closes connection exception", e);
					}
				} else {
					connection.getGenerator().reset();
				}
				break;
			default:
				throw new IllegalStateException(
						"server response does not support the http version " + connection.getHttpVersion());
			}
		}

		@Override
		protected void generateHTTPMessageExceptionally() {
			connection.getGenerator().reset();
			throw new IllegalStateException("server generates http message exception.");
		}

		@Override
		protected ByteBuffer getHeaderByteBuffer() {
			return BufferUtils.allocate(connection.getHTTP2Configuration().getMaxRequestHeadLength());
		}

		@Override
		protected Session getSession() {
			return connection.getTcpSession();
		}

		@Override
		protected HttpGenerator getHttpGenerator() {
			return connection.getGenerator();
		}

	}
}
