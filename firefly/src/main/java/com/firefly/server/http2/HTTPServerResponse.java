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
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTPServerResponse extends MetaData.Response {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

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

	public void response100Continue() {
		try {
			outputStream.response100Continue();
		} catch (IOException e) {
			log.error("the server session {} sends 100 continue unsuccessfully", e);
		}
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

		void response100Continue() throws IOException {
			ByteBuffer header = getHeaderByteBuffer();
			HttpGenerator gen = getHttpGenerator();
			HttpGenerator.Result result = gen.generateResponse(HttpGenerator.CONTINUE_100_INFO, header, null, null,
					false);
			if (result == HttpGenerator.Result.FLUSH && gen.getState() == HttpGenerator.State.COMPLETING_1XX) {
				getSession().encode(header);
				result = gen.generateResponse(null, null, null, null, false);
				if (result == HttpGenerator.Result.DONE && gen.getState() == HttpGenerator.State.START) {
					log.debug("the server session {} sends 100 continue successfully", getSession().getSessionId());
				} else {
					generateHTTPMessageExceptionally(result, gen.getState());
				}
			} else {
				generateHTTPMessageExceptionally(result, gen.getState());
			}

		}

		@Override
		protected void generateHTTPMessageSuccessfully() {
			log.debug("server session {} generates the HTTP message completely", connection.getSessionId());

			final HTTPServerResponse response = (HTTPServerResponse) info;
			final HTTPServerRequest request = response.request;

			String requestConnectionValue = request.getFields().get(HttpHeader.CONNECTION);
			String responseConnectionValue = response.getFields().get(HttpHeader.CONNECTION);

			connection.getGenerator().reset();

			switch (request.getVersion()) {
			case HTTP_1_0:
				if ("keep-alive".equalsIgnoreCase(requestConnectionValue)
						&& "keep-alive".equalsIgnoreCase(responseConnectionValue)) {
					log.debug("the server {} connection {} is persistent", response.getVersion(),
							connection.getSessionId());
				} else {
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
					try {
						connection.close();
					} catch (IOException e) {
						log.error("server closes connection exception", e);
					}
				} else {
					log.debug("the server {} connection {} is persistent", response.getVersion(),
							connection.getSessionId());
				}
				break;
			default:
				throw new IllegalStateException(
						"server response does not support the http version " + connection.getHttpVersion());
			}

		}

		@Override
		protected void generateHTTPMessageExceptionally(HttpGenerator.Result generatorResult,
				HttpGenerator.State generatorState) {
			if (log.isDebugEnabled()) {
				log.debug("http1 generator error, the result is {}, and the generator state is {}", generatorResult,
						generatorState);
			}
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
