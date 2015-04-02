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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + flags;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + version;
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
		ControlFrame other = (ControlFrame) obj;
		if (flags != other.flags)
			return false;
		if (type != other.type)
			return false;
		if (version != other.version)
			return false;
		return true;
	}

}
