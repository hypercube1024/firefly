package com.firefly.db;

public class DBException extends RuntimeException {
	private static final long serialVersionUID = 943860025205853547L;

	public DBException(String message) {
		super(message);
	}

	public DBException(Throwable cause) {
		super(cause);
	}

	public DBException(String message, Throwable cause) {
		super(message, cause);
	}

}
