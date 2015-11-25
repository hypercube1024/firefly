package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.net.Handler;
import com.firefly.net.Session;

public class HTTP2ClientHandler implements Handler {
	
	private final HTTP2Configuration config;
	private final Listener listener;

	public HTTP2ClientHandler(HTTP2Configuration config, Listener listener) {
		this.config = config;
		this.listener = listener;
	}

	@Override
	public void sessionOpened(Session session) throws Throwable {
		session.attachObject(new HTTP2ClientConnection(config, session, null, listener));
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		HTTP2ClientConnection attachment = (HTTP2ClientConnection) session.getAttachment();
		if(attachment != null) {
			attachment.close();
		}
	}

	@Override
	public void messageRecieved(Session session, Object message) throws Throwable {
		// TODO Auto-generated method stub

	}

	@Override
	public void exceptionCaught(Session session, Throwable t) throws Throwable {
		// TODO Auto-generated method stub

	}

}
