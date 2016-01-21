package com.firefly.net;

import com.firefly.net.tcp.ssl.SSLSession;

public interface SSLEventHandler {
	public void handshakeFinished(SSLSession sslSession);
}
