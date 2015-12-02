package com.firefly.client.http2;

import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.utils.concurrent.Promise;

public class HTTP2ClientContext {
	public Promise<HTTP2ClientConnection> promise;
	public Listener listener;
	public volatile String serverSelectedProtocol = "h2";
}
