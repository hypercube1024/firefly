package com.firefly.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.firefly.net.buffer.FileRegion;

public interface Session {
	int CLOSE = 0;
	int OPEN = 1;
	String CLOSE_FLAG = "#firefly-session-close";

	void setAttribute(String key, Object value);

	Object getAttribute(String key);

	void removeAttribute(String key);

	void clearAttributes();

	void fireReceiveMessage(Object message);

	void encode(Object message);

	void write(ByteBuffer byteBuffer);
	
	void write(FileRegion fileRegion);

	int getInterestOps();

	int getSessionId();

	long getOpenTime();
	
	long getLastReadTime();
	
	long getLastWrittenTime();
	
	long getLastActiveTime();
	
	long getReadBytes();
	
	long getWrittenBytes();

	void close(boolean immediately);

	int getState();

	boolean isOpen();
	
	void cancelTimeoutTask();
	
	boolean isCancelTimeoutTask();

	InetSocketAddress getLocalAddress();

	InetSocketAddress getRemoteAddress();
}
