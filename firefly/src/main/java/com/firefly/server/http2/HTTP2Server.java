package com.firefly.server.http2;

import com.firefly.codec.common.DecoderChain;
import com.firefly.codec.common.EncoderChain;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.core.AbstractLifeCycle;
import com.firefly.net.Server;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;
import com.firefly.utils.log.LogFactory;

public class HTTP2Server extends AbstractLifeCycle {
	
	private final Server server;
	private final String host; 
	private final int port;
	
	public HTTP2Server(String host, int port, HTTP2Configuration http2Configuration, ServerSessionListener listener) {
		if (http2Configuration == null)
			throw new IllegalArgumentException("the http2 configuration is null");
		
		if (listener == null)
			throw new IllegalArgumentException("the http2 server listener is null");
		
		if(host == null)
			throw new IllegalArgumentException("the http2 server host is empty");
		
		this.host = host;
		this.port = port;
		
		DecoderChain decoder;
		EncoderChain encoder;
		
		if (http2Configuration.isSecure()) {
			decoder = new ServerSecureDecoder(new HTTP2ServerDecoder());
			encoder = new HTTP2ServerEncoder(new ServerSecureEncoder());
		} else {
			decoder = new DecoderChain(new HTTP2ServerDecoder());
			encoder = new EncoderChain(new HTTP2ServerEncoder());
		}
		
		this.server = new AsynchronousTcpServer(decoder, encoder, new HTTP2ServerHandler(http2Configuration, listener), http2Configuration.getTcpIdleTimeout());
	}

	@Override
	protected void init() {
		server.start(host, port);
	}

	@Override
	protected void destroy() {
		if(server != null)
			server.shutdown();
		LogFactory.getInstance().shutdown();
	}

}
