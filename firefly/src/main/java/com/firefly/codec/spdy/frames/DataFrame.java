package com.firefly.codec.spdy.frames;

import java.nio.ByteBuffer;

public class DataFrame implements Serialization{
	public static final int HEADER_LENGTH = 8;

    private final int streamId;
    private final byte flags;
    private final int length;
    private final ByteBuffer data;

	public DataFrame(int streamId, byte flags, ByteBuffer data) {
		this.streamId = streamId;
		this.flags = flags;
		this.length = data.remaining();
		this.data = data;
	}

	public int getStreamId() {
		return streamId;
	}

	public byte getFlags() {
		return flags;
	}

	public int getLength() {
		return length;
	}
	
	public ByteBuffer getData() {
		return data;
	}

	public boolean isClose() {
		return (flags & Constants.FLAG_CLOSE) == Constants.FLAG_CLOSE;
	}

	@Override
	public String toString() {
		return "DataFrame [streamId=" + streamId + ", flags=" + flags
				+ ", length=" + length + "]";
	}

	@Override
	public ByteBuffer toByteBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH + length);
		buffer.putInt(streamId & 0x7F_FF_FF_FF);
		int flagsAndLength = flags;
        flagsAndLength <<= 24;
        flagsAndLength += length;
        buffer.putInt(flagsAndLength);
        buffer.put(data);
		return buffer;
	}
    
}
