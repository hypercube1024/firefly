package com.firefly.codec.http2.model;

import java.util.List;

import com.firefly.utils.VerifyUtils;

abstract public class CookieGenerator {

    public static String generateCookies(List<Cookie> cookies) {
        if (cookies == null) {
            throw new IllegalArgumentException("the cookie list is null");
        }

        if (cookies.size() == 1) {
            return generateCookie(cookies.get(0));
        } else if (cookies.size() > 1) {
            StringBuilder sb = new StringBuilder();

            sb.append(generateCookie(cookies.get(0)));
            for (int i = 1; i < cookies.size(); i++) {
                sb.append(';').append(generateCookie(cookies.get(i)));
            }

            return sb.toString();
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
            StringBuilder sb = new StringBuilder();

            sb.append(cookie.getName()).append('=').append(cookie.getValue());

            if (VerifyUtils.isNotEmpty(cookie.getComment())) {
                sb.append(";Comment=").append(cookie.getComment());
            }

            if (VerifyUtils.isNotEmpty(cookie.getDomain())) {
                sb.append(";Domain=").append(cookie.getDomain());
            }
            if (cookie.getMaxAge() >= 0) {
                sb.append(";Max-Age=").append(cookie.getMaxAge());
            }

            String path = VerifyUtils.isEmpty(cookie.getPath()) ? "/" : cookie.getPath();
            sb.append(";Path=").append(path);

            if (cookie.getSecure()) {
                sb.append(";Secure");
            }

            sb.append(";Version=").append(cookie.getVersion());

            return sb.toString();
        }
    }

    public static String generateServletSetCookie(javax.servlet.http.Cookie cookie) {
        if (cookie == null) {
            throw new IllegalArgumentException("the cookie is null");
        } else {
            StringBuilder sb = new StringBuilder();

            sb.append(cookie.getName()).append('=').append(cookie.getValue());

            if (VerifyUtils.isNotEmpty(cookie.getComment())) {
                sb.append(";Comment=").append(cookie.getComment());
            }

            if (VerifyUtils.isNotEmpty(cookie.getDomain())) {
                sb.append(";Domain=").append(cookie.getDomain());
            }
            if (cookie.getMaxAge() >= 0) {
                sb.append(";Max-Age=").append(cookie.getMaxAge());
            }

            String path = VerifyUtils.isEmpty(cookie.getPath()) ? "/" : cookie.getPath();
            sb.append(";Path=").append(path);

            if (cookie.getSecure()) {
                sb.append(";Secure");
            }

            sb.append(";Version=").append(cookie.getVersion());

            return sb.toString();
        }
    }
}
