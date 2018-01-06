package com.firefly.net;

/**
 * The net event callback
 *
 * @author qiupengtao
 */
public interface NetEvent {

    void notifySessionOpened(Session session);

    void notifyMessageReceived(Session session, Object message);

    void notifySessionClosed(Session session);

    void notifyExceptionCaught(Session session, Throwable t);

}