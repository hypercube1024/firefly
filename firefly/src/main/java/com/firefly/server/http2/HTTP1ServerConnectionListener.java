package com.firefly.server.http2;

public interface HTTP1ServerConnectionListener {

	public HTTP1ServerRequestHandler onNewConnectionIsCreating();
}
