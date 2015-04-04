package com.firefly.codec.spdy.frames.control;


public enum SessionStatus {

	OK(0),
	PROTOCOL_ERROR(1),
	INTERNAL_ERROR(2);

	public static SessionStatus from(int code) {
		return Codes.codes[code];
	}

	private final int code;

	private SessionStatus(int code) {
		this.code = code;
		Codes.codes[code] = this;
	}

	public int getCode() {
		return code;
	}

	private static class Codes {
		private static final SessionStatus[] codes = new SessionStatus[3];
	}
}
