package com.firefly.net.support.wrap.client;

public interface MessageReceivedCallback {
	void messageRecieved(TcpConnection connection, Object obj);
}
