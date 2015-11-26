package com.firefly.client.http2;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.net.tcp.ssl.SSLEventHandler;

public class HTTP2ClientContext {
	private Listener listener;
	private SSLEventHandler sslEventHandler;
	private ALPN.ClientProvider clientProvider;
}
