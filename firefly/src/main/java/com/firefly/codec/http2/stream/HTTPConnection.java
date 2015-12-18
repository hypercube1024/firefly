package com.firefly.codec.http2.stream;

import java.io.Closeable;

import com.firefly.codec.http2.model.HttpVersion;

public interface HTTPConnection extends Closeable {

	HttpVersion getHttpVersion();

	Object getAttachment();

	void setAttachment(Object attachment);

	boolean isOpen();

}