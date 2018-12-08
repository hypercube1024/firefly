package com.fireflysource.net.http.model;

import com.fireflysource.common.collection.map.MultiMap;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpURITest {

    @Test
    void testInvalidAddress() {
        assertInvalidURI("http://[ffff::1:8080/", "Invalid URL; no closing ']' -- should throw exception");
        assertInvalidURI("**", "only '*', not '**'");
        assertInvalidURI("*/", "only '*', not '*/'");
    }

    private void assertInvalidURI(String invalidURI, String message) {
        HttpURI uri = new HttpURI();
        assertThrows(IllegalArgumentException.class, () -> uri.parse(invalidURI), message);
    }

    @Test
    void testParse() {
        HttpURI uri = new HttpURI();

        uri.parse("*");
        assertNull(uri.getHost());
        assertEquals("*", uri.getPath());

        uri.parse("/foo/bar");
        assertNull(uri.getHost());
        assertEquals("/foo/bar", uri.getPath());

        uri.parse("//foo/bar");
        assertEquals("foo", uri.getHost());
        assertEquals("/bar", uri.getPath());

        uri.parse("http://foo/bar");
        assertEquals("foo", uri.getHost());
        assertEquals("/bar", uri.getPath());
    }

    @Test
    void testParseRequestTarget() {
        HttpURI uri = new HttpURI();

        uri.parseRequestTarget("GET", "*");
        assertNull(uri.getHost());
        assertEquals("*", uri.getPath());

        uri.parseRequestTarget("GET", "/foo/bar");
        assertNull(uri.getHost());
        assertEquals("/foo/bar", uri.getPath());

        uri.parseRequestTarget("GET", "//foo/bar");
        assertNull(uri.getHost());
        assertEquals("//foo/bar", uri.getPath());

        uri.parseRequestTarget("GET", "http://foo/bar");
        assertEquals("foo", uri.getHost());
        assertEquals("/bar", uri.getPath());
    }

    @Test
    void testExtB() throws Exception {
        for (String value : new String[]{"a", "abcdABCD", "\u00C0", "\u697C", "\uD869\uDED5", "\uD840\uDC08"}) {
            HttpURI uri = new HttpURI("/path?value=" + URLEncoder.encode(value, "UTF-8"));

            MultiMap<String> parameters = new MultiMap<>();
            uri.decodeQueryTo(parameters, StandardCharsets.UTF_8);
            assertEquals(value, parameters.getString("value"));
        }
    }

    @Test
    void testAt() {
        HttpURI uri = new HttpURI("/@foo/bar");
        assertEquals("/@foo/bar", uri.getPath());
    }

    @Test
    void testParams() {
        HttpURI uri = new HttpURI("/foo/bar");
        assertEquals("/foo/bar", uri.getPath());
        assertEquals("/foo/bar", uri.getDecodedPath());
        assertNull(uri.getParam());

        uri = new HttpURI("/foo/bar;jsessionid=12345");
        assertEquals("/foo/bar;jsessionid=12345", uri.getPath());
        assertEquals("/foo/bar", uri.getDecodedPath());
        assertEquals("jsessionid=12345", uri.getParam());

        uri = new HttpURI("/foo;abc=123/bar;jsessionid=12345");
        assertEquals("/foo;abc=123/bar;jsessionid=12345", uri.getPath());
        assertEquals("/foo/bar", uri.getDecodedPath());
        assertEquals("jsessionid=12345", uri.getParam());

        uri = new HttpURI("/foo;abc=123/bar;jsessionid=12345?name=value");
        assertEquals("/foo;abc=123/bar;jsessionid=12345", uri.getPath());
        assertEquals("/foo/bar", uri.getDecodedPath());
        assertEquals("jsessionid=12345", uri.getParam());

        uri = new HttpURI("/foo;abc=123/bar;jsessionid=12345#target");
        assertEquals("/foo;abc=123/bar;jsessionid=12345", uri.getPath());
        assertEquals("/foo/bar", uri.getDecodedPath());
        assertEquals("jsessionid=12345", uri.getParam());

        uri = new HttpURI("/trainingCamp/poster.jpeg;jsessionid=12345?id=420");
        assertEquals("/trainingCamp/poster.jpeg;jsessionid=12345", uri.getPath());
        assertEquals("jsessionid=12345", uri.getParam());
        assertEquals("/trainingCamp/poster.jpeg", uri.getDecodedPath());
    }

    @Test
    void testMutableURI() {
        HttpURI uri = new HttpURI("/foo/bar");
        assertEquals("/foo/bar", uri.toString());
        assertEquals("/foo/bar", uri.getPath());
        assertEquals("/foo/bar", uri.getDecodedPath());

        uri.setScheme("http");
        assertEquals("http:/foo/bar", uri.toString());
        assertEquals("/foo/bar", uri.getPath());
        assertEquals("/foo/bar", uri.getDecodedPath());

        uri.setAuthority("host", 0);
        assertEquals("http://host/foo/bar", uri.toString());
        assertEquals("/foo/bar", uri.getPath());
        assertEquals("/foo/bar", uri.getDecodedPath());

        uri.setAuthority("host", 8888);
        assertEquals("http://host:8888/foo/bar", uri.toString());
        assertEquals("/foo/bar", uri.getPath());
        assertEquals("/foo/bar", uri.getDecodedPath());

        uri.setPathQuery("/f%30%30;p0/bar;p1;p2");
        assertEquals("http://host:8888/f%30%30;p0/bar;p1;p2", uri.toString());
        assertEquals("/f%30%30;p0/bar;p1;p2", uri.getPath());
        assertEquals("/f00/bar", uri.getDecodedPath());
        assertEquals("p2", uri.getParam());
        assertEquals(null, uri.getQuery());

        uri.setPathQuery("/f%30%30;p0/bar;p1;p2?name=value");
        assertEquals("http://host:8888/f%30%30;p0/bar;p1;p2?name=value", uri.toString());
        assertEquals("/f%30%30;p0/bar;p1;p2", uri.getPath());
        assertEquals("/f00/bar", uri.getDecodedPath());
        assertEquals("p2", uri.getParam());
        assertEquals("name=value", uri.getQuery());

        uri.setQuery("other=123456");
        assertEquals("http://host:8888/f%30%30;p0/bar;p1;p2?other=123456", uri.toString());
        assertEquals("/f%30%30;p0/bar;p1;p2", uri.getPath());
        assertEquals("/f00/bar", uri.getDecodedPath());
        assertEquals("p2", uri.getParam());
        assertEquals("other=123456", uri.getQuery());
    }

    @Test
    void testSchemeAndOrAuthority() {
        HttpURI uri = new HttpURI("/path/info");
        assertEquals("/path/info", uri.toString());

        uri.setAuthority("host", 0);
        assertEquals("//host/path/info", uri.toString());

        uri.setAuthority("host", 8888);
        assertEquals("//host:8888/path/info", uri.toString());

        uri.setScheme("http");
        assertEquals("http://host:8888/path/info", uri.toString());

        uri.setAuthority(null, 0);
        assertEquals("http:/path/info", uri.toString());

    }

    @Test
    void testBasicAuthCredentials() {
        HttpURI uri = new HttpURI("http://user:password@example.com:8888/blah");
        assertEquals("http://user:password@example.com:8888/blah", uri.toString());
        assertEquals("example.com:8888", uri.getAuthority());
        assertEquals("user:password", uri.getUser());
    }
}
