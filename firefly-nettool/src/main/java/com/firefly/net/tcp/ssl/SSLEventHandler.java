package com.firefly.net.tcp.ssl;

public interface SSLEventHandler {
	void handshakeFinished(SSLSession sslSession);
}
