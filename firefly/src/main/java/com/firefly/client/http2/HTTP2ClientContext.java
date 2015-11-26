package com.firefly.client.http2;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.net.tcp.ssl.SSLEventHandler;

public class HTTP2ClientContext {
	public Listener listener;
	public SSLEventHandler sslEventHandler;
	public ALPN.ClientProvider clientProvider;
}
