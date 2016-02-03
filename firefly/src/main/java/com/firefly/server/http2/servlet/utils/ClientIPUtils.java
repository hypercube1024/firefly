package com.firefly.server.http2.servlet.utils;

import javax.servlet.http.HttpServletRequest;

import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

abstract public class ClientIPUtils {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static String getRemoteAddr(HttpServletRequest request) {
		String remoteAddr = parseRemoteAddr(request.getHeader(HttpHeader.X_FORWARDED_FOR.asString()));
		if (remoteAddr != null) {
			return remoteAddr;
		} else {
			return request.getRemoteAddr();
		}
	}

	public static String parseRemoteAddr(String remoteAddr) {
		try {
			if (VerifyUtils.isNotEmpty(remoteAddr)) {
				if (remoteAddr.contains(",")) {
					String[] array = StringUtils.split(remoteAddr, ',');
					for (String element : array) {
						String addr = element.trim();
						if (!"unknown".equalsIgnoreCase(addr)) {
							return addr;
						}
					}
					return null;
				} else {
					return remoteAddr.trim();
				}
			} else {
				return null;
			}
		} catch (Throwable e) {
			log.error("parse romote ip error", e);
			return null;
		}
	}
}
