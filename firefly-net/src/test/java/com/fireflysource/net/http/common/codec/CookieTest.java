package com.fireflysource.net.http.common.codec;


import com.fireflysource.net.http.common.model.Cookie;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieTest {

    @Test
    void setCookieTest() {
        Cookie cookie = new Cookie("test31", "hello");
        cookie.setDomain("www.fireflysource.com");
        cookie.setPath("/test/hello");
        cookie.setMaxAge(10);
        cookie.setSecure(true);
        cookie.setComment("commenttest");
        cookie.setVersion(20);

        String setCookieString = CookieGenerator.generateSetCookie(cookie);

        Cookie setCookie = CookieParser.parseSetCookie(setCookieString);
        assertEquals("test31", setCookie.getName());
        assertEquals("hello", setCookie.getValue());
        assertEquals("www.fireflysource.com", setCookie.getDomain());
        assertEquals("/test/hello", setCookie.getPath());
        assertTrue(setCookie.getSecure());
        assertEquals("commenttest", setCookie.getComment());
        assertEquals(20, setCookie.getVersion());
    }

    @Test
    void cookieTest() {
        Cookie cookie = new Cookie("test21", "hello");
        String cookieString = CookieGenerator.generateCookie(cookie);

        List<Cookie> list = CookieParser.parseCookie(cookieString);
        assertEquals(1, list.size());
        assertEquals("test21", list.get(0).getName());
        assertEquals("hello", list.get(0).getValue());
    }

    @Test
    void cookieListTest() {
        List<Cookie> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new Cookie("test" + i, "hello" + i));
        }
        String cookieString = CookieGenerator.generateCookies(list);

        List<Cookie> ret = CookieParser.parseCookie(cookieString);
        assertEquals(10, ret.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("test" + i, ret.get(i).getName());
            assertEquals("hello" + i, ret.get(i).getValue());
        }
    }
}
