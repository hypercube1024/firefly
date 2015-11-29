package com.firefly.client.http2;

import com.firefly.codec.common.Promise;
import com.firefly.codec.http2.stream.Session.Listener;

public class HTTP2ClientContext {
	public Promise<HTTP2ClientConnection> promise;
	public Listener listener;
	public volatile String serverSelectedProtocol = "h2";
}
