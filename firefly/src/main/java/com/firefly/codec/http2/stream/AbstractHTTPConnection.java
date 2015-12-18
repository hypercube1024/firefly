package com.firefly.codec.http2.stream;

import java.io.IOException;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

public class AbstractHTTPConnection implements HTTPConnection {
	
	protected volatile Object attachment;
	protected final SSLSession sslSession;
	protected final Session tcpSession;
	protected final HttpVersion httpVersion;

	public AbstractHTTPConnection(SSLSession sslSession, Session tcpSession, HttpVersion httpVersion) {
		this.sslSession = sslSession;
		this.tcpSession = tcpSession;
		this.httpVersion = httpVersion;
	}
	
	@Override
	public HttpVersion getHttpVersion() {
		return httpVersion;
	}
	
	@Override
	public Object getAttachment() {
		return attachment;
	}

	@Override
	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	@Override
	public boolean isOpen() {
		return tcpSession.isOpen();
	}
	
	@Override
	public void close() throws IOException {
		attachment = null;
		if (sslSession != null && sslSession.isOpen()) {
			sslSession.close();
		}
		if(tcpSession != null && tcpSession.isOpen()) {
			tcpSession.close();
		}
	}

}
