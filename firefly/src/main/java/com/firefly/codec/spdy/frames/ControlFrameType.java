package com.firefly.codec.spdy.frames;

import java.util.HashMap;
import java.util.Map;

public enum ControlFrameType {
	SYN_STREAM((short) 1), 
	SYN_REPLY((short) 2), 
	RST_STREAM((short) 3), 
	SETTINGS((short) 4), 
	NOOP((short) 5), 
	PING((short) 6), 
	GO_AWAY((short) 7), 
	HEADERS((short) 8), 
	WINDOW_UPDATE((short) 9), 
	CREDENTIAL((short) 10);

	public static ControlFrameType from(short code) {
		return Codes.codes.get(code);
	}

	private final short code;

	private ControlFrameType(short code) {
		this.code = code;
		Codes.codes.put(code, this);
	}

	public short getCode() {
		return code;
	}

	private static class Codes {
		private static final Map<Short, ControlFrameType> codes = new HashMap<>();
	}
}
