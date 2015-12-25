package com.firefly.codec.http2.model;

import java.util.ArrayList;
import java.util.List;

import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;

abstract public class CookieParser {

	public static Cookie parseSetCookie(String cookieStr) {
		if (VerifyUtils.isEmpty(cookieStr)) {
			throw new IllegalArgumentException("the cookie string is empty");
		} else {
			Cookie cookie = new Cookie();
			String[] cookieKeyValues = StringUtils.split(cookieStr, ';');

			for (String cookieKeyValue : cookieKeyValues) {
				String[] kv = StringUtils.split(cookieKeyValue, "=", 2);
				if (kv != null) {
					if (kv.length == 2) {
						String name = kv[0];
						String value = kv[1];
						if ("Comment".equalsIgnoreCase(name)) {
							cookie.setComment(value);
						} else if ("Domain".equalsIgnoreCase(name)) {
							cookie.setDomain(value);
						} else if ("Max-Age".equalsIgnoreCase(name)) {
							cookie.setMaxAge(Integer.parseInt(value));
						} else if ("Path".equalsIgnoreCase(name)) {
							cookie.setPath(value);
						} else if ("Secure".equalsIgnoreCase(name)) {
							cookie.setSecure(true);
						} else if ("Version".equalsIgnoreCase(name)) {
							cookie.setVersion(Integer.parseInt(value));
						} else {
							cookie.setName(name);
							cookie.setValue(value);
						}
					} else if (kv.length == 1) {
						String name = kv[0];
						if ("Secure".equalsIgnoreCase(name)) {
							cookie.setSecure(true);
						}
					} else {
						throw new IllegalStateException("the cookie string format error");
					}
				} else {
					throw new IllegalStateException("the cookie string format error");
				}
			}
			return cookie;
		}
	}

	public static List<Cookie> parseCookie(String cookieStr) {
		if (VerifyUtils.isEmpty(cookieStr)) {
			throw new IllegalArgumentException("the cookie string is empty");
		} else {
			List<Cookie> list = new ArrayList<Cookie>();
			String[] cookieKeyValues = StringUtils.split(cookieStr, ';');
			for (String cookieKeyValue : cookieKeyValues) {
				String[] kv = StringUtils.split(cookieKeyValue, "=", 2);
				if (kv != null) {
					if (kv.length == 2) {
						list.add(new Cookie(kv[0], kv[1]));
					} else if (kv.length == 1) {
						list.add(new Cookie(kv[0], ""));
					} else {
						throw new IllegalStateException("the cookie string format error");
					}
				} else {
					throw new IllegalStateException("the cookie string format error");
				}
			}
			return list;
		}
	}
}
