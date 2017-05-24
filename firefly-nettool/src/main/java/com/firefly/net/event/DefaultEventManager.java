package com.firefly.net.event;

import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It is the callback of net event in Worker's thread.
 * @author qiupengtao
 *
 */
public class DefaultEventManager implements EventManager {
	private static Logger log = LoggerFactory.getLogger("firefly-system");
	private Config config;

	public DefaultEventManager(Config config) {
		log.info("create default event manager");
		this.config = config;
	}

	@Override
	public void executeCloseTask(Session session) {
		try {
			config.getHandler().sessionClosed(session);
		} catch (Throwable t) {
			executeExceptionTask(session, t);
		}
	}

	@Override
	public void executeExceptionTask(Session session, Throwable t) {
		try {
			config.getHandler().exceptionCaught(session, t);
		} catch (Throwable t0) {
			log.error("handler exception", t0);
		}

	}

	@Override
	public void executeOpenTask(Session session) {
		try {
			config.getHandler().sessionOpened(session);
		} catch (Throwable t) {
			executeExceptionTask(session, t);
		}
	}

	@Override
	public void executeReceiveTask(Session session, Object message) {
		try {
			log.debug("CurrentThreadEventManager");
			config.getHandler().messageReceived(session, message);
		} catch (Throwable t) {
			executeExceptionTask(session, t);
		}
	}

	@Override
	public void shutdown() {
		
	}
}
