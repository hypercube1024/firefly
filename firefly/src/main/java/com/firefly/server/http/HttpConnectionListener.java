package com.firefly.server.http;

import java.util.EventListener;

import com.firefly.net.Session;

public interface HttpConnectionListener extends EventListener {
	void connectionCreated(Session session);

	void connectionClosed(Session session);
}
