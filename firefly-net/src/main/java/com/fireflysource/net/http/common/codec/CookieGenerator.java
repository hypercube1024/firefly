package com.fireflysource.net.http.common.codec;

import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.http.common.model.Cookie;

import java.util.List;

abstract public class CookieGenerator {

    public static String generateCookies(List<Cookie> cookies) {
        if (cookies == null) {
            throw new IllegalArgumentException("the cookie list is null");
        }

        if (cookies.size() == 1) {
            return generateCookie(cookies.get(0));
        } else if (cookies.size() > 1) {
            StringBuilder str = new StringBuilder();

            str.append(generateCookie(cookies.get(0)));
            for (int i = 1; i < cookies.size(); i++) {
                str.append(';').append(generateCookie(cookies.get(i)));
            }

            return str.toString();
        } else {
            throw new IllegalArgumentException("the cookie list size is 0");
        }
    }

    public static String generateCookie(Cookie cookie) {
        if (cookie == null) {
            throw new IllegalArgumentException("the cookie is null");
        } else {
            return cookie.getName() + '=' + cookie.getValue();
        }
    }

    public static String generateSetCookie(Cookie cookie) {
        if (cookie == null) {
            throw new IllegalArgumentException("the cookie is null");
        } else {
            StringBuilder str = new StringBuilder();

            str.append(cookie.getName()).append('=').append(cookie.getValue());

            if (StringUtils.hasText(cookie.getComment())) {
                str.append(";Comment=").append(cookie.getComment());
            }

            if (StringUtils.hasText(cookie.getDomain())) {
                str.append(";Domain=").append(cookie.getDomain());
            }
            if (cookie.getMaxAge() >= 0) {
                str.append(";Max-Age=").append(cookie.getMaxAge());
            }

            String path = !StringUtils.hasText(cookie.getPath()) ? "/" : cookie.getPath();
            str.append(";Path=").append(path);

            if (cookie.getSecure()) {
                str.append(";Secure");
            }

            str.append(";Version=").append(cookie.getVersion());

            return str.toString();
        }
    }


}
