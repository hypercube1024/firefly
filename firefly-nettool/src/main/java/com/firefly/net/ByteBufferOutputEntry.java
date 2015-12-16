package com.firefly.net;

import java.nio.ByteBuffer;

import com.firefly.utils.concurrent.Callback;

public class ByteBufferOutputEntry extends AbstractOutputEntry<ByteBuffer> {

	public ByteBufferOutputEntry(Callback callback, ByteBuffer data) {
		super(callback, data);
	}

	@Override
	public OutputEntryType getOutputEntryType() {
		return OutputEntryType.BYTE_BUFFER;
	}

}
