package com.fireflysource.net.http.common.codec;


import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.http.common.model.Cookie;

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
            switch (name.toLowerCase()) {
                case "comment":
                    cookie.setComment(value);
                    break;
                case "domain":
                    cookie.setDomain(value);
                    break;
                case "max-age":
                    cookie.setMaxAge(Integer.parseInt(value));
                    break;
                case "secure":
                    cookie.setSecure(true);
                    break;
                case "version":
                    cookie.setVersion(Integer.parseInt(value));
                    break;
                default:
                    cookie.setName(name);
                    cookie.setValue(value);
                    break;
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
