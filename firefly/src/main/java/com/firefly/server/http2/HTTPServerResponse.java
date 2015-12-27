package com.firefly.server.http2;

import java.io.IOException;
import java.io.OutputStream;

import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.net.Session;

public class HTTPServerResponse extends MetaData.Response {

	protected final HTTP1ServerResponseOutputStream outputStream;

	public HTTPServerResponse(HTTP1ServerConnection connection) {
		super(HttpVersion.HTTP_1_1, 0, new HttpFields());
		outputStream = new HTTP1ServerResponseOutputStream(this, connection);
	}

	public HTTP1ServerResponseOutputStream getOutputStream() {
		return outputStream;
	}

	public static class HTTP1ServerResponseOutputStream extends OutputStream {

		private final HTTPServerResponse response;
		private final HTTP1ServerConnection connection;
		private boolean closed;
		private boolean commited;

		public HTTP1ServerResponseOutputStream(HTTPServerResponse response, HTTP1ServerConnection connection) {
			this.response = response;
			this.connection = connection;
		}

		public HTTP1ServerConnection getConnection() {
			return connection;
		}

		public boolean isClosed() {
			return closed;
		}

		public boolean isCommited() {
			return commited;
		}

		@Override
		public void write(int b) throws IOException {
			// TODO Auto-generated method stub
		}

		@Override
		public synchronized void write(byte[] array, int offset, int length) throws IOException {
			if (closed)
				return;

			if (response.getStatus() <= 0)
				throw new IllegalStateException(
						"The http1 server response must set the status, current status is " + response.getStatus());

			final HttpGenerator generator = connection.getGenerator();
			final Session tcpSession = connection.getTcpSession();
			HttpGenerator.Result generatorResult;

			if (!commited) {

			} else {

			}
			// TODO
		}

		@Override
		public synchronized void close() throws IOException {
			if (closed)
				return;

			if (!commited) {

			} else {

			}
			// TODO
		}

	}
}
