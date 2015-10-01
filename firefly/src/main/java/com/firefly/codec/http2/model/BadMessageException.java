package com.firefly.codec.http2.model;

public class BadMessageException extends RuntimeException {
	private static final long serialVersionUID = -4907256166019479626L;
	final int _code;
	final String _reason;

	public BadMessageException() {
		this(400, null);
	}

	public BadMessageException(int code) {
		this(code, null);
	}

	public BadMessageException(String reason) {
		this(400, reason);
	}

	public BadMessageException(int code, String reason) {
		super(code + ": " + reason);
		_code = code;
		_reason = reason;
	}

	public BadMessageException(int code, String reason, Throwable cause) {
		super(code + ": " + reason, cause);
		_code = code;
		_reason = reason;
	}

	public int getCode() {
		return _code;
	}

	public String getReason() {
		return _reason;
	}
}
