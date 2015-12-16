package com.firefly.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;

public interface Session {
	
	public static final DisconnectionOutputEntry DISCONNECTION_FLAG = new DisconnectionOutputEntry(null, null);
	
	public static final int CLOSE = 0;
	public static final int OPEN = 1;

	void attachObject(Object attachment);
	
	Object getAttachment();

	void fireReceiveMessage(Object message);

	void encode(Object message);
	
	void write(OutputEntry<?> entry);
	
	void write(ByteBuffer byteBuffer, Callback callback);
	
	void write(ByteBuffer[] buffers, Callback callback);
	
	void write(Collection<ByteBuffer> buffers, Callback callback);
	
	void write(FileRegion file, Callback callback);

	int getSessionId();

	long getOpenTime();
	
	long getLastReadTime();
	
	long getLastWrittenTime();
	
	long getLastActiveTime();
	
	long getReadBytes();
	
	long getWrittenBytes();

	void close();
	
	void closeNow();

	int getState();

	boolean isOpen();

	InetSocketAddress getLocalAddress();

	InetSocketAddress getRemoteAddress();
}
