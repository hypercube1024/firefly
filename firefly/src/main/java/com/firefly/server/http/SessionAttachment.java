package com.firefly.server.http;

import java.nio.ByteBuffer;

import com.firefly.net.tcp.ssl.SSLSession;

public class SessionAttachment {
	public SSLSession sslSession;
	public ByteBuffer byteBuffer;
	public HttpServletRequestImpl req;
}
