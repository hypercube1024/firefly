package com.firefly.server.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.core.AbstractLifeCycle;
import com.firefly.net.DecoderChain;
import com.firefly.net.EncoderChain;
import com.firefly.net.Server;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;
import com.firefly.utils.log.LogFactory;

public class HTTP2Server extends AbstractLifeCycle {

	private final Server server;
	private final String host;
	private final int port;

	public HTTP2Server(String host, int port, HTTP2Configuration http2Configuration,
			ServerHTTPHandler serverHTTPHandler) {
		this(host, port, http2Configuration, new HTTP2ServerRequestHandler(serverHTTPHandler), serverHTTPHandler);
	}

	public HTTP2Server(String host, int port, HTTP2Configuration http2Configuration, ServerSessionListener listener,
			ServerHTTPHandler serverHTTPHandler) {
		if (http2Configuration == null)
			throw new IllegalArgumentException("the http2 configuration is null");

		if (host == null)
			throw new IllegalArgumentException("the http2 server host is empty");

		this.host = host;
		this.port = port;

		DecoderChain decoder;
		EncoderChain encoder;

		if (http2Configuration.isSecure()) {
			decoder = new ServerSecureDecoder(new HTTP1ServerDecoder(new HTTP2ServerDecoder()));
			encoder = new HTTP1ServerEncoder(new HTTP2ServerEncoder(new ServerSecureEncoder()));
		} else {
			decoder = new HTTP1ServerDecoder(new HTTP2ServerDecoder());
			encoder = new HTTP1ServerEncoder(new HTTP2ServerEncoder());
		}

		this.server = new AsynchronousTcpServer(decoder, encoder,
				new HTTP2ServerHandler(http2Configuration, listener, serverHTTPHandler),
				http2Configuration.getTcpIdleTimeout());
	}

	@Override
	protected void init() {
		server.start(host, port);
	}

	@Override
	protected void destroy() {
		if (server != null)
			server.shutdown();
		LogFactory.getInstance().shutdown();
	}

}
