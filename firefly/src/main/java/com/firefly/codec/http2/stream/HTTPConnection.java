package com.firefly.codec.http2.stream;

import java.io.Closeable;
import java.net.InetSocketAddress;

import com.firefly.codec.http2.model.HttpVersion;

public interface HTTPConnection extends Closeable {

	public HttpVersion getHttpVersion();

	public Object getAttachment();

	public void setAttachment(Object attachment);

	public boolean isOpen();
	
	public boolean isEncrypted();
	
	public int getSessionId();
	
	public InetSocketAddress getLocalAddress();

	public InetSocketAddress getRemoteAddress();

}