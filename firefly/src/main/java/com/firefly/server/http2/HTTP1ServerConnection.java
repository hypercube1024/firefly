package com.firefly.server.http2;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.decode.SettingsBodyParser;
import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PrefaceFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.BadMessageException;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpHeaderValue;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.codec.http2.stream.AbstractHTTP1Connection;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.codec.Base64Utils;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.lang.TypeUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP1ServerConnection extends AbstractHTTP1Connection {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	final ServerSessionListener serverSessionListener;
	boolean upgradeHTTP2Successfully = false;

	public HTTP1ServerConnection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession,
			HTTP1ServerRequestHandler requestHandler, ServerSessionListener serverSessionListener) {
		super(config, sslSession, tcpSession, requestHandler, null);
		requestHandler.connection = this;
		this.serverSessionListener = serverSessionListener;
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

	boolean upgradeProtocolToHTTP2(HTTPServerRequest request, HTTPServerResponse response) {
		if (HttpMethod.PRI.is(request.getMethod())) {
			// TODO need to test
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
								response.responseH2c();

								HTTP2ServerConnection http2ServerConnection = new HTTP2ServerConnection(config,
										tcpSession, sslSession, serverSessionListener);
								tcpSession.attachObject(http2ServerConnection);

								// TODO need to test
								http2ServerConnection.getParser().standardUpgrade();

								serverSessionListener.onAccept(http2ServerConnection.getHttp2Session());
								((HTTP2ServerSession) http2ServerConnection.getHttp2Session())
										.onFrame(new PrefaceFrame());
								((HTTP2ServerSession) http2ServerConnection.getHttp2Session()).onFrame(settingsFrame);
								((HTTP2ServerSession) http2ServerConnection.getHttp2Session())
										.onFrame(new HeadersFrame(1, request, null, true));
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
