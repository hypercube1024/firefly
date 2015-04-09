package com.firefly.codec.spdy.frames;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataFrame implements Serialization {
	public static final int HEADER_LENGTH = 8;
	public static final byte FLAG_FIN = 1;

    private final int streamId;
    private final byte flags;
    private int length;
    private byte[] data;

	public DataFrame(int streamId, byte flags) {
		this.streamId = streamId;
		this.flags = flags;
	}
	
	public DataFrame(int streamId, byte flags, byte[] data) {
		this(streamId, flags);
		this.data = data;
	}

	public int getStreamId() {
		return streamId;
	}

	public byte getFlags() {
		return flags;
	}

	public int getDataSize() {
		return data != null ? data.length : 0;
	}
	
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
		if(data != null)
			this.length = data.length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}

	public int getLength() {
		return length;
	}

	public boolean isClose() {
		return (flags & FLAG_FIN) == FLAG_FIN;
	}

	@Override
	public String toString() {
		return "DataFrame [streamId=" + streamId + ", flags=" + flags
				+ ", length=" + getDataSize() + "]";
	}

	@Override
	public ByteBuffer toByteBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH + getDataSize());
		buffer.putInt(streamId & 0x7F_FF_FF_FF);
		int flagsAndLength = flags;
        flagsAndLength <<= 24;
        flagsAndLength += getDataSize();
        buffer.putInt(flagsAndLength);
        if(data != null)
        	buffer.put(data);
        buffer.flip();
		return buffer;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(data);
		result = prime * result + flags;
		result = prime * result + streamId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataFrame other = (DataFrame) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		if (flags != other.flags)
			return false;
		if (streamId != other.streamId)
			return false;
		return true;
	}
    
}
