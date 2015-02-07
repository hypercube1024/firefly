package com.firefly.codec.spdy.frames.control;

import java.util.HashMap;
import java.util.Map;

public enum SessionStatus {

	OK(0),
	PROTOCOL_ERROR(1),
	INTERNAL_ERROR(2);

	/**
	 * @param code
	 *            the session status code
	 * @return a {@link SessionStatus} from the given code, or null if no status
	 *         exists
	 */
	public static SessionStatus from(int code) {
		return Codes.codes.get(code);
	}

	private final int code;

	private SessionStatus(int code) {
		this.code = code;
		Codes.codes.put(code, this);
	}

	public int getCode() {
		return code;
	}

	private static class Codes {
		private static final Map<Integer, SessionStatus> codes = new HashMap<>();
	}
}
