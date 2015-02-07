package com.firefly.codec.spdy.frames.exception;

import com.firefly.codec.spdy.frames.control.SessionStatus;

public class SessionException extends RuntimeException {

	private static final long serialVersionUID = -8972561691220698156L;
	
	private final SessionStatus sessionStatus;

	public SessionException(SessionStatus sessionStatus) {
		this.sessionStatus = sessionStatus;
	}

	public SessionException(SessionStatus sessionStatus, String message) {
		super(message);
		this.sessionStatus = sessionStatus;
	}

	public SessionException(SessionStatus sessionStatus, Throwable cause) {
		super(cause);
		this.sessionStatus = sessionStatus;
	}

	public SessionStatus getSessionStatus() {
		return sessionStatus;
	}
}
