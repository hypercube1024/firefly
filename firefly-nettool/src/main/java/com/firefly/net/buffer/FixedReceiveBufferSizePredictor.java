package com.firefly.net.buffer;

import com.firefly.net.ReceiveBufferSizePredictor;


public class FixedReceiveBufferSizePredictor implements
		ReceiveBufferSizePredictor {

	private final int bufferSize;

	public FixedReceiveBufferSizePredictor(int bufferSize) {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException(
					"bufferSize must greater than 0: " + bufferSize);
		}
		this.bufferSize = bufferSize;
	}

	@Override
	public int nextReceiveBufferSize() {
		return bufferSize;
	}

	@Override
	public void previousReceiveBufferSize(int previousReceiveBufferSize) {
		// Ignore
	}

}
