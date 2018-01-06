package com.firefly.net.event;

import com.firefly.net.Config;
import com.firefly.net.NetEvent;
import com.firefly.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute callback in net threads
 *
 * @author qiupengtao
 */
public class DefaultNetEvent implements NetEvent {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private Config config;

    public DefaultNetEvent(Config config) {
        log.info("create default event manager");
        this.config = config;
    }

    @Override
    public void notifySessionClosed(Session session) {
        try {
            config.getHandler().sessionClosed(session);
        } catch (Throwable t) {
            notifyExceptionCaught(session, t);
        }
    }

    @Override
    public void notifyExceptionCaught(Session session, Throwable t) {
        try {
            config.getHandler().exceptionCaught(session, t);
        } catch (Throwable t0) {
            log.error("handler exception", t0);
        }

    }

    @Override
    public void notifySessionOpened(Session session) {
        try {
            config.getHandler().sessionOpened(session);
        } catch (Throwable t) {
            notifyExceptionCaught(session, t);
        }
    }

    @Override
    public void notifyMessageReceived(Session session, Object message) {
        try {
            log.debug("CurrentThreadEventManager");
            config.getHandler().messageReceived(session, message);
        } catch (Throwable t) {
            notifyExceptionCaught(session, t);
        }
    }

}
