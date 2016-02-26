package com.firefly.server.utils;

import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class StatisticsUtils {

	private static Log accessLog = LogFactory.getInstance().getLog("firefly-access");
	private static Log connectionLog = LogFactory.getInstance().getLog("firefly-connection");

	public static void saveRequestInfo(Object sid, Object remoteAddr, String method, Object uri, long timeDiff) {
		accessLog.info("request: [sessionId={}, remoteAddr={}, method={}, uri={}, timeDiff={}]", sid, remoteAddr, method, uri,
				timeDiff);
	}

	public static void saveConnectionInfo(Session session) {
		connectionLog.info("session: {}", session);
	}
}
