package com.firefly.server.http2;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.decode.SettingsBodyParser;
import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PrefaceFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.AbstractHTTP1Connection;
import com.firefly.codec.http2.stream.AbstractHTTP1OutputStream;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.SessionSPI;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.codec.Base64Utils;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.lang.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HTTP1ServerConnection extends AbstractHTTP1Connection implements HTTPServerConnection {

	protected static final Logger log = LoggerFactory.getLogger("firefly-system");

	final ServerSessionListener serverSessionListener;
	final HTTP1ServerRequestHandler serverRequestHandler;
	boolean upgradeHTTP2Successfully = false;

	public HTTP1ServerConnection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession,
			HTTP1ServerRequestHandler requestHandler, ServerSessionListener serverSessionListener) {
		super(config, sslSession, tcpSession, requestHandler, null);
		requestHandler.connection = this;
		this.serverSessionListener = serverSessionListener;
		this.serverRequestHandler = requestHandler;
	}

	@Override
	protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler,
			ResponseHandler responseHandler) {
		return new HttpParser(requestHandler, config.getMaxRequestHeadLength());
	}

	@Override
	protected HttpGenerator initHttpGenerator() {
		return new HttpGenerator(true, true);
	}

	HttpParser getParser() {
		return parser;
	}

	HttpGenerator getGenerator() {
		return generator;
	}

	SSLSession getSSLSession() {
		return sslSession;
	}

	Session getTcpSession() {
		return tcpSession;
	}

	HTTP2Configuration getHTTP2Configuration() {
		return config;
	}

	public MetaData.Request getRequest() {
		return serverRequestHandler.request;
	}

	public MetaData.Response getResponse() {
		return serverRequestHandler.response;
	}

	public void response100Continue() {
		try {
			serverRequestHandler.outputStream.response100Continue();
		} catch (IOException e) {
			log.error("the server session {} sends 100 continue unsuccessfully", e);
		}
	}

	void responseH2c() {
		try {
			serverRequestHandler.outputStream.responseH2c();
		} catch (IOException e) {
			log.error("the server session {} sends 101 switching protocols unsuccessfully", e);
		}
	}

	static class HTTP1ServerResponseOutputStream extends AbstractHTTP1OutputStream {

		private static final MetaData.Response H2C_RESPONSE = new MetaData.Response(HttpVersion.HTTP_1_1, 101,
				new HttpFields());

		static {
			H2C_RESPONSE.getFields().put(HttpHeader.CONNECTION, HttpHeaderValue.UPGRADE);
			H2C_RESPONSE.getFields().put(HttpHeader.UPGRADE, "h2c");
		}

		private final HTTP1ServerConnection connection;

		public HTTP1ServerResponseOutputStream(MetaData.Response response, HTTP1ServerConnection connection) {
			super(response, false);
			this.connection = connection;
		}

		HTTP1ServerConnection getHTTP1ServerConnection() {
			return connection;
		}

		void responseH2c() throws IOException {
			ByteBuffer header = getHeaderByteBuffer();
			HttpGenerator gen = getHttpGenerator();
			HttpGenerator.Result result = gen.generateResponse(H2C_RESPONSE, false, header, null, null, true);
			if (result == HttpGenerator.Result.FLUSH && gen.getState() == HttpGenerator.State.COMPLETING) {
				getSession().encode(header);
				result = gen.generateResponse(null, false, null, null, null, true);
				if (result == HttpGenerator.Result.DONE && gen.getState() == HttpGenerator.State.END) {
					log.debug("the server session {} sends 101 switching protocols successfully",
							getSession().getSessionId());
				} else {
					generateHTTPMessageExceptionally(result, gen.getState());
				}
			} else {
				generateHTTPMessageExceptionally(result, gen.getState());
			}
		}

		void response100Continue() throws IOException {
			ByteBuffer header = getHeaderByteBuffer();
			HttpGenerator gen = getHttpGenerator();
			HttpGenerator.Result result = gen.generateResponse(HttpGenerator.CONTINUE_100_INFO, false, header, null, null,
					false);
			if (result == HttpGenerator.Result.FLUSH && gen.getState() == HttpGenerator.State.COMPLETING_1XX) {
				getSession().encode(header);
				result = gen.generateResponse(null, false, null, null, null, false);
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

			final MetaData.Response response = connection.getResponse();
			final MetaData.Request request = connection.getRequest();

			String requestConnectionValue = request.getFields().get(HttpHeader.CONNECTION);
			String responseConnectionValue = response.getFields().get(HttpHeader.CONNECTION);

			connection.getGenerator().reset();

			switch (request.getHttpVersion()) {
			case HTTP_1_0:
				if ("keep-alive".equalsIgnoreCase(requestConnectionValue)
						&& "keep-alive".equalsIgnoreCase(responseConnectionValue)) {
					log.debug("the server {} connection {} is persistent", response.getHttpVersion(),
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
					log.debug("the server {} connection {} is persistent", response.getHttpVersion(),
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

	boolean upgradeProtocolToHTTP2(MetaData.Request request, MetaData.Response response) {
		if (HttpMethod.PRI.is(request.getMethod())) {
			// XXX need test
			HTTP2ServerConnection http2ServerConnection = new HTTP2ServerConnection(config, tcpSession, sslSession,
					serverSessionListener);
			tcpSession.attachObject(http2ServerConnection);
			http2ServerConnection.getParser().directUpgrade();
			upgradeHTTP2Successfully = true;
			return true;
		} else {
			HttpField connectionField = request.getFields().getField(HttpHeader.CONNECTION);
			if (connectionField != null) {
				if (connectionField.contains("Upgrade")) {
					if (log.isDebugEnabled()) {
						log.debug("the server will upgrade protocol {}", request.getFields());
					}

					if (request.getFields().contains(HttpHeader.UPGRADE, "h2c")) {
						HttpField settingsField = request.getFields().getField(HttpHeader.HTTP2_SETTINGS);
						if (settingsField != null) {
							response.setStatus(101);
							response.getFields().put(HttpHeader.CONNECTION, HttpHeaderValue.UPGRADE);
							response.getFields().put(HttpHeader.UPGRADE, "h2c");

							final byte[] settings = Base64Utils.decodeFromUrlSafeString(settingsField.getValue());
							if (log.isDebugEnabled()) {
								log.debug("the server received settings {}", TypeUtils.toHexString(settings));
							}

							SettingsFrame settingsFrame = SettingsBodyParser.parseBody(BufferUtils.toBuffer(settings));
							if (settingsFrame == null) {
								throw new BadMessageException("settings frame parsing error");
							} else {
								responseH2c();

								HTTP2ServerConnection http2ServerConnection = new HTTP2ServerConnection(config,
										tcpSession, sslSession, serverSessionListener);
								tcpSession.attachObject(http2ServerConnection);

								http2ServerConnection.getParser().standardUpgrade();

								serverSessionListener.onAccept(http2ServerConnection.getHttp2Session());
								SessionSPI sessionSPI = http2ServerConnection.getSessionSPI();

								sessionSPI.onFrame(new PrefaceFrame());
								sessionSPI.onFrame(settingsFrame);
								sessionSPI.onFrame(new HeadersFrame(1, request, null, true));
							}

							upgradeHTTP2Successfully = true;
							return true;
						} else {
							throw new IllegalStateException("upgrade HTTP2 unsuccessfully");
						}
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}

}
