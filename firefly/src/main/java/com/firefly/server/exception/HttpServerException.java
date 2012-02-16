package com.firefly.server.exception;

@SuppressWarnings("serial")
public class HttpServerException extends RuntimeException {
	public HttpServerException(String msg) {
		super(msg);
	}
}
