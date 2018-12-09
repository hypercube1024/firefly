package com.fireflysource.net.http.common.codec;


import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.http.model.Cookie;

import java.util.ArrayList;
import java.util.List;

abstract public class CookieParser {

    public static void parseCookies(String cookieStr, CookieParserCallback callback) {
        if (!StringUtils.hasText(cookieStr)) {
            throw new IllegalArgumentException("the cookie string is empty");
        } else {
            String[] cookieKeyValues = StringUtils.split(cookieStr, ';');
            for (String cookieKeyValue : cookieKeyValues) {
                String[] kv = StringUtils.split(cookieKeyValue, "=", 2);
                if (kv != null) {
                    if (kv.length == 2) {
                        callback.cookie(kv[0].trim(), kv[1].trim());
                    } else if (kv.length == 1) {
                        callback.cookie(kv[0].trim(), "");
                    } else {
                        throw new IllegalStateException("the cookie string format error");
                    }
                } else {
                    throw new IllegalStateException("the cookie string format error");
                }
            }
        }
    }

    public static Cookie parseSetCookie(String cookieStr) {
        final Cookie cookie = new Cookie();
        parseCookies(cookieStr, (name, value) -> {
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

        });
        return cookie;
    }

    public static List<Cookie> parseCookie(String cookieStr) {
        final List<Cookie> list = new ArrayList<>();
        parseCookies(cookieStr, (name, value) -> list.add(new Cookie(name, value)));
        return list;
    }

    public interface CookieParserCallback {
        void cookie(String name, String value);
    }

}
