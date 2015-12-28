package com.firefly.codec.http2.stream;

import java.io.Closeable;
import java.net.InetSocketAddress;

import com.firefly.codec.http2.model.HttpVersion;

public interface HTTPConnection extends Closeable {

	HttpVersion getHttpVersion();

	Object getAttachment();

	void setAttachment(Object attachment);

	boolean isOpen();
	
	boolean isEncrypted();
	
	int getSessionId();
	
	InetSocketAddress getLocalAddress();

	InetSocketAddress getRemoteAddress();

}