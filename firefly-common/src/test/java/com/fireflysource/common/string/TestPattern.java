package com.fireflysource.common.string;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestPattern {

    @Test
    @DisplayName("should match pattern successfully.")
    void testPattern() {
        Pattern p = Pattern.compile("?ddaaad?", "?");
        assertEquals("", p.match("ddaaad")[1]);
        assertEquals("xwww", p.match("ddaaadxwww")[1]);
        assertEquals("xwww", p.match("addaaadxwww")[1]);
        assertEquals("a", p.match("addaaadxwww")[0]);
        assertEquals("a", p.match("addaaad")[0]);
        assertNull(p.match("orange"));

        p = Pattern.compile("?", "?");
        assertEquals("orange", p.match("orange")[0]);

        p = Pattern.compile("??????", "?");
        assertEquals("orange", p.match("orange")[0]);
        assertEquals(1, p.match("orange").length);

        p = Pattern.compile("org", "?");
        assertNull(p.match("orange"));
        assertEquals(0, p.match("org").length);

        p = Pattern.compile("?org", "?");
        assertEquals("", p.match("org")[0]);
        assertEquals("aass", p.match("aassorg")[0]);
        assertEquals(1, p.match("ssorg").length);

        p = Pattern.compile("org?", "?");
        assertEquals("", p.match("org")[0]);
        assertEquals("aaa", p.match("orgaaa")[0]);
        assertEquals(1, p.match("orgaaa").length);

        p = Pattern.compile("www.?.com?", "?");
        assertEquals("fireflysource", p.match("www.fireflysource.com")[0]);
        assertEquals("", p.match("www.fireflysource.com")[1]);
        assertEquals("/cn/", p.match("www.fireflysource.com/cn/")[1]);
        assertEquals(2, p.match("www.fireflysource.com/cn/").length);
        assertNull(p.match("orange"));

        p = Pattern.compile("www.?.com/?/app", "?");
        assertNull(p.match("orange"));
        assertEquals(2, p.match("www.fireflysource.com/cn/app").length);
        assertEquals("fireflysource", p.match("www.fireflysource.com/cn/app")[0]);
        assertEquals("cn", p.match("www.fireflysource.com/cn/app")[1]);

        p = Pattern.compile("?www.?.com/?/app", "?");
        assertNull(p.match("orange"));
        assertEquals(3, p.match("www.fireflysource.com/cn/app").length);
        assertEquals("", p.match("www.fireflysource.com/cn/app")[0]);
        assertEquals("fireflysource", p.match("www.fireflysource.com/cn/app")[1]);
        assertEquals("cn", p.match("www.fireflysource.com/cn/app")[2]);
        assertEquals("http://", p.match("http://www.fireflysource.com/cn/app")[0]);

        p = Pattern.compile("?www.?.com/?/app?", "?");
        assertNull(p.match("orange"));
        assertEquals(4, p.match("www.fireflysource.com/cn/app").length);
        assertEquals("", p.match("www.fireflysource.com/cn/app")[0]);
        assertEquals("fireflysource", p.match("www.fireflysource.com/cn/app")[1]);
        assertEquals("cn", p.match("www.fireflysource.com/cn/app")[2]);
        assertEquals("http://", p.match("http://www.fireflysource.com/cn/app")[0]);
        assertEquals("", p.match("http://www.fireflysource.com/cn/app")[3]);
        assertEquals("/1334", p.match("http://www.fireflysource.com/cn/app/1334")[3]);

        p = Pattern.compile("abc*abc", "*");
        assertEquals("", p.match("abcabcabc")[0]);

        p = Pattern.compile("aa*aa", "*");
        assertEquals("", p.match("aaaaa")[0]);

        p = Pattern.compile("*.mustache", "*");
        assertNull(p.match("IO.class"));
    }

}
