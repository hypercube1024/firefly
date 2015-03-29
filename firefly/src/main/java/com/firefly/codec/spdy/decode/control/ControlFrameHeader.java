package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.exception.NoImplementMethodException;
import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;

public class ControlFrameHeader extends ControlFrame {

	private final int length;
	
	public ControlFrameHeader(short version, ControlFrameType type, byte flags, int length) {
		super(version, type, flags);
		this.length = length;
	}

	public int getLength() {
		return length;
	}

	@Override
	public ByteBuffer toByteBuffer() {
		throw new NoImplementMethodException("This method is not implemented");
	}

}
