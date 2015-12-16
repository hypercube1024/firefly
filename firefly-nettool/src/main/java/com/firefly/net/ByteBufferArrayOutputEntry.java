package com.firefly.net;

import java.nio.ByteBuffer;

import com.firefly.utils.concurrent.Callback;

public class ByteBufferArrayOutputEntry extends AbstractOutputEntry<ByteBuffer[]> {

	public ByteBufferArrayOutputEntry(Callback callback, ByteBuffer[] data) {
		super(callback, data);
	}

	@Override
	public OutputEntryType getOutputEntryType() {
		return OutputEntryType.BYTE_BUFFER_ARRAY;
	}

}
