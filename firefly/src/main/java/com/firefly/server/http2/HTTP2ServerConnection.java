package com.firefly.server.http2;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.decode.ServerParser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.stream.AbstractHTTP2Connection;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTP2Session;
import com.firefly.codec.http2.stream.SessionSPI;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

public class HTTP2ServerConnection extends AbstractHTTP2Connection implements ServerHTTPConnection {

	public HTTP2ServerConnection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession,
			ServerSessionListener serverSessionListener) {
		super(config, tcpSession, sslSession, serverSessionListener);
		if (serverSessionListener instanceof HTTP2ServerRequestHandler) {
			HTTP2ServerRequestHandler handler = (HTTP2ServerRequestHandler) serverSessionListener;
			handler.connection = this;
		}
	}

	protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
			Listener listener) {
		HTTP2ServerSession http2ServerSession = new HTTP2ServerSession(scheduler, this.tcpSession, this.generator,
				(ServerSessionListener) listener, flowControl, config.getStreamIdleTimeout());
		http2ServerSession.setMaxLocalStreams(config.getMaxConcurrentStreams());
		http2ServerSession.setMaxRemoteStreams(config.getMaxConcurrentStreams());
		return http2ServerSession;
	}

	protected Parser initParser(HTTP2Configuration config) {
		return new ServerParser((HTTP2ServerSession) http2Session, config.getMaxDynamicTableSize(),
				config.getMaxRequestHeadLength());
	}

	ServerParser getParser() {
		return (ServerParser) parser;
	}

	Generator getGenerator() {
		return generator;
	}

	SSLSession getSSLSession() {
		return sslSession;
	}

	SessionSPI getSessionSPI() {
		return http2Session;
	}
}
