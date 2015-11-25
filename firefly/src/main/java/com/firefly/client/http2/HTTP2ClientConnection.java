package com.firefly.client.http2;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.stream.AbstractHTTP2Connection;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

public class HTTP2ClientConnection extends AbstractHTTP2Connection {

	public HTTP2ClientConnection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession,
			Listener listener) {
		super(config, tcpSession, sslSession, listener);
	}

	protected Parser initParser(HTTP2Configuration config, FlowControlStrategy flowControl, Listener listener) {
		HTTP2ClientSession http2ClientSession = new HTTP2ClientSession(scheduler, this.tcpSession, this.generator,
				listener, flowControl, config.getStreamIdleTimeout());
		return new Parser(http2ClientSession, config.getMaxDynamicTableSize(), config.getMaxRequestHeadLength());
	}

}
