package com.firefly.net;

/**
 * It is the callback of net event
 * @author qiupengtao
 *
 */
public interface EventManager {
	void executeOpenTask(Session session);
	void executeReceiveTask(Session session, Object message);
	void executeCloseTask(Session session);
	void executeExceptionTask(Session session, Throwable t);
	void shutdown();
}