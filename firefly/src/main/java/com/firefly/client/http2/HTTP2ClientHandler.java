package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.net.Handler;
import com.firefly.net.Session;

public class HTTP2ClientHandler implements Handler {
	
	private final HTTP2Configuration config;
	
	public HTTP2ClientHandler(HTTP2Configuration config) {
		this.config = config;
	}

	@Override
	public void sessionOpened(Session session) throws Throwable {
		new HTTP2ClientConnection(config, session, null, null);
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
