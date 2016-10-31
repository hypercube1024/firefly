package com.firefly.server.utils;

import com.firefly.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StatisticsUtils {

	private static Logger access = LoggerFactory.getLogger("firefly-access");
	private static Logger monitor = LoggerFactory.getLogger("firefly-monitor");

	public static void saveRequestInfo(Object sid, Object remoteAddr, String method, Object uri, long timeDiff) {
		access.info("request: [sessionId={}, remoteAddr={}, method={}, uri={}, timeDiff={}]", sid, remoteAddr, method, uri,
				timeDiff);
	}

	public static void saveConnectionInfo(Session session) {
		monitor.info("session: {}", session);
	}
	
}
