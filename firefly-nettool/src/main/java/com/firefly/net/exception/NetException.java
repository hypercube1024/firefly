package com.firefly.net.exception;

public class NetException extends RuntimeException {

	private static final long serialVersionUID = 5751160039001031850L;
	
	public NetException(String msg) {
		super(msg);
	}
}
