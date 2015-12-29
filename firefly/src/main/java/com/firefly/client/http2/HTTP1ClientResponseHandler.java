package com.firefly.client.http2;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.firefly.client.http2.HTTP1ClientConnection.HTTP1ClientRequestOutputStream;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTP2Session;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class HTTP1ClientResponseHandler implements ResponseHandler {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	protected HTTP1ClientConnection connection;
	protected HTTPClientResponse response;
	protected HTTPClientRequest request;
	protected Promise<HTTPConnection> promise;
	protected Listener listener;
	protected Promise<Stream> initStream;
	protected Stream.Listener initStreamListener;

	HTTP1ClientRequestOutputStream continueOutput;

	@Override
	public final boolean startResponse(HttpVersion version, int status, String reason) {
		if (log.isDebugEnabled()) {
			log.debug("client received the response line, {}, {}, {}", version, status, reason);
		}

		if (status == 100 && "Continue".equalsIgnoreCase(reason)) {
			try {
				continueToSendData(continueOutput, connection);
				if (log.isDebugEnabled()) {
					log.debug("client received 100 continue, current parser state is {}",
							connection.getParser().getState());
				}
				return true;
			} finally {
				try {
					continueOutput.close();
				} catch (IOException e) {
					log.error("client generates the HTTP message exception", e);
				}
				continueOutput = null;
			}
		} else {
			response = new HTTPClientResponse(version, status, reason);
			return false;
		}
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

	protected boolean http1MessageComplete() {
		try {
			return messageComplete(response, connection);
		} finally {
			// if(connection.upgradeHTTP2Successfully) {
			// log.debug("the client connection {} has upgraded HTTP2",
			// connection.getSessionId());
			// return true;
			// }

			String requestConnectionValue = request.getFields().get(HttpHeader.CONNECTION);
			String responseConnectionValue = response.getFields().get(HttpHeader.CONNECTION);

			connection.getParser().reset();

			switch (response.getVersion()) {
			case HTTP_1_0:
				if ("keep-alive".equalsIgnoreCase(requestConnectionValue)
						&& "keep-alive".equalsIgnoreCase(responseConnectionValue)) {
					log.debug("the client {} connection is persistent", response.getVersion());
				} else {
					try {
						connection.close();
					} catch (IOException e) {
						log.error("client closes connection exception", e);
					}
				}
				break;
			case HTTP_1_1: // the persistent connection is default in HTTP 1.1
				if ("close".equalsIgnoreCase(requestConnectionValue)
						|| "close".equalsIgnoreCase(responseConnectionValue)) {
					try {
						connection.close();
					} catch (IOException e) {
						log.error("client closes connection exception", e);
					}
				} else {
					log.debug("the client {} connection is persistent", response.getVersion());
				}
				break;
			default:
				throw new IllegalStateException(
						"client response does not support the http version " + connection.getHttpVersion());
			}

		}
	}

	@Override
	public final boolean messageComplete() {
		if (promise != null && listener != null) {
			String upgradeValue = response.getFields().get(HttpHeader.UPGRADE);
			if (response.getStatus() == HttpStatus.SWITCHING_PROTOCOLS_101 && "h2c".equalsIgnoreCase(upgradeValue)) {
				connection.upgradeHTTP2Successfully = true;

				// initialize http2 client connection;
				final HTTP2ClientConnection http2Connection = new HTTP2ClientConnection(
						connection.getHTTP2Configuration(), connection.getTcpSession(), null, listener) {
					@Override
					protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
							Listener listener) {
						return HTTP2ClientSession.initSessionForUpgradingHTTP2(scheduler, this.tcpSession, generator,
								listener, flowControl, 3, config.getStreamIdleTimeout(), initStream,
								initStreamListener);
					}
				};
				connection.getTcpSession().attachObject(http2Connection);
				http2Connection.initialize(connection.getHTTP2Configuration(), promise, listener);
				return http1MessageComplete();
			} else {
				return http1MessageComplete();
			}
		} else {
			return http1MessageComplete();
		}
	}

	@Override
	public final void badMessage(int status, String reason) {
		badMessage(status, reason, response, connection);
	}

	abstract public void continueToSendData(HTTP1ClientRequestOutputStream output, HTTP1ClientConnection connection);

	abstract public boolean content(ByteBuffer item, HTTPClientResponse response, HTTP1ClientConnection connection);

	abstract public boolean headerComplete(HTTPClientResponse response, HTTP1ClientConnection connection);

	abstract public boolean messageComplete(HTTPClientResponse response, HTTP1ClientConnection connection);

	abstract public void badMessage(int status, String reason, HTTPClientResponse response,
			HTTP1ClientConnection connection);

	public static class Adapter extends HTTP1ClientResponseHandler {

		@Override
		public void earlyEOF() {
		}

		@Override
		public boolean content(ByteBuffer item, HTTPClientResponse response, HTTP1ClientConnection connection) {
			return false;
		}

		@Override
		public boolean headerComplete(HTTPClientResponse response, HTTP1ClientConnection connection) {
			return false;
		}

		@Override
		public boolean messageComplete(HTTPClientResponse response, HTTP1ClientConnection connection) {
			return true;
		}

		@Override
		public void badMessage(int status, String reason, HTTPClientResponse response,
				HTTP1ClientConnection connection) {
		}

		@Override
		public void continueToSendData(HTTP1ClientRequestOutputStream output, HTTP1ClientConnection connection) {
		}

	}

}
