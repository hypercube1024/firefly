package com.firefly.codec.spdy.frames;

public class DataFrame {
	public static final int HEADER_LENGTH = 8;

    private final int streamId;
    private final byte flags;
    private final int length;
    
	public DataFrame(int streamId, byte flags, int length) {
		super();
		this.streamId = streamId;
		this.flags = flags;
		this.length = length;
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
	
	public boolean isClose() {
		return (flags & Constants.FLAG_CLOSE) == Constants.FLAG_CLOSE;
	}

	@Override
	public String toString() {
		return "DataFrame [streamId=" + streamId + ", flags=" + flags
				+ ", length=" + length + "]";
	}
    
}
