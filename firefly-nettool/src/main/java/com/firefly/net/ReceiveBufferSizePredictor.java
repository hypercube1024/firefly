package com.firefly.net;

public interface ReceiveBufferSizePredictor {
	int nextReceiveBufferSize();
	
	void previousReceiveBufferSize(int previousReceiveBufferSize);
}
