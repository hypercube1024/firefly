package com.firefly.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.firefly.net.buffer.FileRegion;

public interface Session {
	int CLOSE = 0;
	int OPEN = 1;
	String CLOSE_FLAG = "#firefly-session-close";

	void attachObject(Object attachment);
	
	Object getAttachment();

	void fireReceiveMessage(Object message);

	void encode(Object message);

	void write(ByteBuffer byteBuffer);
	
	void write(FileRegion fileRegion);

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

	InetSocketAddress getLocalAddress();

	InetSocketAddress getRemoteAddress();
}
