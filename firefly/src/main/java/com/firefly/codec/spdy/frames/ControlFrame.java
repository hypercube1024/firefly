package com.firefly.codec.spdy.frames;

import java.nio.ByteBuffer;

abstract public class ControlFrame implements Serialization {
	public static final int HEADER_LENGTH = 8;
	public static final int VERSION = 3;

	private final short version;
	private final ControlFrameType type;
	private final byte flags;

	public ControlFrame(short version, ControlFrameType type, byte flags) {
		super();
		this.version = version;
		this.type = type;
		this.flags = flags;
	}

	public short getVersion() {
		return version;
	}

	public ControlFrameType getType() {
		return type;
	}

	public byte getFlags() {
		return flags;
	}
	
	protected void generateControlFrameHeader(int frameLength, ByteBuffer buffer) {
        buffer.putShort((short)(0x8000 + version));
        buffer.putShort(type.getCode());
        int flagsAndLength = flags;
        flagsAndLength <<= 24;
        flagsAndLength += frameLength;
        buffer.putInt(flagsAndLength);
    }

	@Override
	public String toString() {
		return "ControlFrame [version=" + version + ", type=" + type
				+ ", flags=" + flags + "]";
	}

}
