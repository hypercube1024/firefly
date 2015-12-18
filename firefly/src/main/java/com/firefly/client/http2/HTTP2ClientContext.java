package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.utils.concurrent.Promise;

public class HTTP2ClientContext {
	public Promise<HTTPConnection> promise;
	public Listener listener;
	public volatile String serverSelectedProtocol = "http/1.1";
}
