package com.firefly.template.exception;

public class ExpressionError extends RuntimeException {

	private static final long serialVersionUID = -5607969690821083756L;

	public ExpressionError(String msg) {
		super(msg);
	}
}
