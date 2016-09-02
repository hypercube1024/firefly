package com.firefly.net;

public interface Handler {

	void sessionOpened(Session session) throws Throwable;

	void sessionClosed(Session session) throws Throwable;

	void messageRecieved(Session session, Object message) throws Throwable;

	void exceptionCaught(Session session, Throwable t) throws Throwable;

	default void failedOpeningSession(Integer sessionId, Throwable t) throws Throwable {

	}

	default void failedAcceptingSession(Integer sessionId, Throwable t) throws Throwable {

	}
}
