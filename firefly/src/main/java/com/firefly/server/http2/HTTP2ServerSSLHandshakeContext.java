package com.firefly.server.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.tcp.ssl.SSLSession;

public class HTTP2ServerSSLHandshakeContext {
	public volatile SSLSession sslSession;
	public volatile HttpVersion httpVersion = HttpVersion.HTTP_1_1;
}
