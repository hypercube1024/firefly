package com.firefly.net;

/**
 * The net event callback
 *
 * @author qiupengtao
 */
public interface EventManager {

    void executeOpenTask(Session session);

    void executeReceiveTask(Session session, Object message);

    void executeCloseTask(Session session);

    void executeExceptionTask(Session session, Throwable t);

}