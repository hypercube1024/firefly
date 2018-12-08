package com.fireflysource.net.http.model;

import com.fireflysource.net.http.common.codec.URIUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Util meta Tests.
 */
class URIUtilsTest {

    @Test
    void testEncodePath() {
        // test basic encode/decode
        StringBuilder buf = new StringBuilder();

        buf.setLength(0);
        URIUtils.encodePath(buf, "/foo%23+;,:=/b a r/?info ");
        assertEquals("/foo%2523+%3B,:=/b%20a%20r/%3Finfo%20", buf.toString());

        assertEquals("/foo%2523+%3B,:=/b%20a%20r/%3Finfo%20", URIUtils.encodePath("/foo%23+;,:=/b a r/?info "));

        buf.setLength(0);
        URIUtils.encodeString(buf, "foo%23;,:=b a r", ";,= ");
        assertEquals("foo%2523%3b%2c:%3db%20a%20r", buf.toString());

        buf.setLength(0);
        URIUtils.encodePath(buf, "/context/'list'/\"me\"/;<script>window.alert('xss');</script>");
        assertEquals("/context/%27list%27/%22me%22/%3B%3Cscript%3Ewindow.alert(%27xss%27)%3B%3C/script%3E", buf.toString());

        buf.setLength(0);
        URIUtils.encodePath(buf, "test\u00f6?\u00f6:\u00df");
        assertEquals("test%C3%B6%3F%C3%B6:%C3%9F", buf.toString());

        buf.setLength(0);
        URIUtils.encodePath(buf, "test?\u00f6?\u00f6:\u00df");
        assertEquals("test%3F%C3%B6%3F%C3%B6:%C3%9F", buf.toString());
    }


    @Test
    void testDecodePath() {
        assertEquals("/foo/bar", URIUtils.decodePath("xx/foo/barxx", 2, 8));
        assertEquals("/foo/bar", URIUtils.decodePath("/foo/bar"));
        assertEquals("/f o/b r", URIUtils.decodePath("/f%20o/b%20r"));
        assertEquals("/foo/bar", URIUtils.decodePath("/foo;ignore/bar;ignore"));
        assertEquals("/fää/bar", URIUtils.decodePath("/f\u00e4\u00e4;ignore/bar;ignore"));
        assertEquals("/f\u0629\u0629%23/bar", URIUtils.decodePath("/f%d8%a9%d8%a9%2523;ignore/bar;ignore"));

        assertEquals("foo%23;,:=b a r", URIUtils.decodePath("foo%2523%3b%2c:%3db%20a%20r;rubbish"));
        assertEquals("/foo/bar%23;,:=b a r=", URIUtils.decodePath("xxx/foo/bar%2523%3b%2c:%3db%20a%20r%3Dxxx;rubbish", 3, 35));
        assertEquals("f\u00e4\u00e4%23;,:=b a r=", URIUtils.decodePath("fää%2523%3b%2c:%3db%20a%20r%3D"));
        assertEquals("f\u0629\u0629%23;,:=b a r", URIUtils.decodePath("f%d8%a9%d8%a9%2523%3b%2c:%3db%20a%20r"));

        // Test for null character (real world ugly test case)
        byte[] oddBytes = {'/', 0x00, '/'};
        String odd = new String(oddBytes, StandardCharsets.ISO_8859_1);
        assertEquals(odd, URIUtils.decodePath("/%00/"));
    }


    @Test
    public void testAddEncodedPaths() {
        assertNull(URIUtils.addEncodedPaths(null, null));
        assertEquals("", URIUtils.addEncodedPaths(null, ""));
        assertEquals("bbb", URIUtils.addEncodedPaths(null, "bbb"));
        assertEquals("/", URIUtils.addEncodedPaths(null, "/"));
        assertEquals("/bbb", URIUtils.addEncodedPaths(null, "/bbb"));

        assertEquals("", URIUtils.addEncodedPaths("", null));
        assertEquals("", URIUtils.addEncodedPaths("", ""));
        assertEquals("bbb", URIUtils.addEncodedPaths("", "bbb"));
        assertEquals("/", URIUtils.addEncodedPaths("", "/"));
        assertEquals("/bbb", URIUtils.addEncodedPaths("", "/bbb"));

        assertEquals("aaa", URIUtils.addEncodedPaths("aaa", null));
        assertEquals("aaa", URIUtils.addEncodedPaths("aaa", ""));
        assertEquals("aaa/bbb", URIUtils.addEncodedPaths("aaa", "bbb"));
        assertEquals("aaa/", URIUtils.addEncodedPaths("aaa", "/"));
        assertEquals("aaa/bbb", URIUtils.addEncodedPaths("aaa", "/bbb"));

        assertEquals("/", URIUtils.addEncodedPaths("/", null));
        assertEquals("/", URIUtils.addEncodedPaths("/", ""));
        assertEquals("/bbb", URIUtils.addEncodedPaths("/", "bbb"));
        assertEquals("/", URIUtils.addEncodedPaths("/", "/"));
        assertEquals("/bbb", URIUtils.addEncodedPaths("/", "/bbb"));

        assertEquals("aaa/", URIUtils.addEncodedPaths("aaa/", null));
        assertEquals("aaa/", URIUtils.addEncodedPaths("aaa/", ""));
        assertEquals("aaa/bbb", URIUtils.addEncodedPaths("aaa/", "bbb"));
        assertEquals("aaa/", URIUtils.addEncodedPaths("aaa/", "/"));
        assertEquals("aaa/bbb", URIUtils.addEncodedPaths("aaa/", "/bbb"));

        assertEquals(";JS", URIUtils.addEncodedPaths(";JS", null));
        assertEquals(";JS", URIUtils.addEncodedPaths(";JS", ""));
        assertEquals("bbb;JS", URIUtils.addEncodedPaths(";JS", "bbb"));
        assertEquals("/;JS", URIUtils.addEncodedPaths(";JS", "/"));
        assertEquals("/bbb;JS", URIUtils.addEncodedPaths(";JS", "/bbb"));

        assertEquals("aaa;JS", URIUtils.addEncodedPaths("aaa;JS", null));
        assertEquals("aaa;JS", URIUtils.addEncodedPaths("aaa;JS", ""));
        assertEquals("aaa/bbb;JS", URIUtils.addEncodedPaths("aaa;JS", "bbb"));
        assertEquals("aaa/;JS", URIUtils.addEncodedPaths("aaa;JS", "/"));
        assertEquals("aaa/bbb;JS", URIUtils.addEncodedPaths("aaa;JS", "/bbb"));

        assertEquals("aaa/;JS", URIUtils.addEncodedPaths("aaa/;JS", null));
        assertEquals("aaa/;JS", URIUtils.addEncodedPaths("aaa/;JS", ""));
        assertEquals("aaa/bbb;JS", URIUtils.addEncodedPaths("aaa/;JS", "bbb"));
        assertEquals("aaa/;JS", URIUtils.addEncodedPaths("aaa/;JS", "/"));
        assertEquals("aaa/bbb;JS", URIUtils.addEncodedPaths("aaa/;JS", "/bbb"));

        assertEquals("?A=1", URIUtils.addEncodedPaths("?A=1", null));
        assertEquals("?A=1", URIUtils.addEncodedPaths("?A=1", ""));
        assertEquals("bbb?A=1", URIUtils.addEncodedPaths("?A=1", "bbb"));
        assertEquals("/?A=1", URIUtils.addEncodedPaths("?A=1", "/"));
        assertEquals("/bbb?A=1", URIUtils.addEncodedPaths("?A=1", "/bbb"));

        assertEquals("aaa?A=1", URIUtils.addEncodedPaths("aaa?A=1", null));
        assertEquals("aaa?A=1", URIUtils.addEncodedPaths("aaa?A=1", ""));
        assertEquals("aaa/bbb?A=1", URIUtils.addEncodedPaths("aaa?A=1", "bbb"));
        assertEquals("aaa/?A=1", URIUtils.addEncodedPaths("aaa?A=1", "/"));
        assertEquals("aaa/bbb?A=1", URIUtils.addEncodedPaths("aaa?A=1", "/bbb"));

        assertEquals("aaa/?A=1", URIUtils.addEncodedPaths("aaa/?A=1", null));
        assertEquals("aaa/?A=1", URIUtils.addEncodedPaths("aaa/?A=1", ""));
        assertEquals("aaa/bbb?A=1", URIUtils.addEncodedPaths("aaa/?A=1", "bbb"));
        assertEquals("aaa/?A=1", URIUtils.addEncodedPaths("aaa/?A=1", "/"));
        assertEquals("aaa/bbb?A=1", URIUtils.addEncodedPaths("aaa/?A=1", "/bbb"));

        assertEquals(";JS?A=1", URIUtils.addEncodedPaths(";JS?A=1", null));
        assertEquals(";JS?A=1", URIUtils.addEncodedPaths(";JS?A=1", ""));
        assertEquals("bbb;JS?A=1", URIUtils.addEncodedPaths(";JS?A=1", "bbb"));
        assertEquals("/;JS?A=1", URIUtils.addEncodedPaths(";JS?A=1", "/"));
        assertEquals("/bbb;JS?A=1", URIUtils.addEncodedPaths(";JS?A=1", "/bbb"));

        assertEquals("aaa;JS?A=1", URIUtils.addEncodedPaths("aaa;JS?A=1", null));
        assertEquals("aaa;JS?A=1", URIUtils.addEncodedPaths("aaa;JS?A=1", ""));
        assertEquals("aaa/bbb;JS?A=1", URIUtils.addEncodedPaths("aaa;JS?A=1", "bbb"));
        assertEquals("aaa/;JS?A=1", URIUtils.addEncodedPaths("aaa;JS?A=1", "/"));
        assertEquals("aaa/bbb;JS?A=1", URIUtils.addEncodedPaths("aaa;JS?A=1", "/bbb"));

        assertEquals("aaa/;JS?A=1", URIUtils.addEncodedPaths("aaa/;JS?A=1", null));
        assertEquals("aaa/;JS?A=1", URIUtils.addEncodedPaths("aaa/;JS?A=1", ""));
        assertEquals("aaa/bbb;JS?A=1", URIUtils.addEncodedPaths("aaa/;JS?A=1", "bbb"));
        assertEquals("aaa/;JS?A=1", URIUtils.addEncodedPaths("aaa/;JS?A=1", "/"));
        assertEquals("aaa/bbb;JS?A=1", URIUtils.addEncodedPaths("aaa/;JS?A=1", "/bbb"));

    }


    @Test
    public void testAddDecodedPaths() {
        assertNull(URIUtils.addPaths(null, null));
        assertEquals("", URIUtils.addPaths(null, ""));
        assertEquals("bbb", URIUtils.addPaths(null, "bbb"));
        assertEquals("/", URIUtils.addPaths(null, "/"));
        assertEquals("/bbb", URIUtils.addPaths(null, "/bbb"));

        assertEquals("", URIUtils.addPaths("", null));
        assertEquals("", URIUtils.addPaths("", ""));
        assertEquals("bbb", URIUtils.addPaths("", "bbb"));
        assertEquals("/", URIUtils.addPaths("", "/"));
        assertEquals("/bbb", URIUtils.addPaths("", "/bbb"));

        assertEquals("aaa", URIUtils.addPaths("aaa", null));
        assertEquals("aaa", URIUtils.addPaths("aaa", ""));
        assertEquals("aaa/bbb", URIUtils.addPaths("aaa", "bbb"));
        assertEquals("aaa/", URIUtils.addPaths("aaa", "/"));
        assertEquals("aaa/bbb", URIUtils.addPaths("aaa", "/bbb"));

        assertEquals("/", URIUtils.addPaths("/", null));
        assertEquals("/", URIUtils.addPaths("/", ""));
        assertEquals("/bbb", URIUtils.addPaths("/", "bbb"));
        assertEquals("/", URIUtils.addPaths("/", "/"));
        assertEquals("/bbb", URIUtils.addPaths("/", "/bbb"));

        assertEquals("aaa/", URIUtils.addPaths("aaa/", null));
        assertEquals("aaa/", URIUtils.addPaths("aaa/", ""));
        assertEquals("aaa/bbb", URIUtils.addPaths("aaa/", "bbb"));
        assertEquals("aaa/", URIUtils.addPaths("aaa/", "/"));
        assertEquals("aaa/bbb", URIUtils.addPaths("aaa/", "/bbb"));

        assertEquals(";JS", URIUtils.addPaths(";JS", null));
        assertEquals(";JS", URIUtils.addPaths(";JS", ""));
        assertEquals(";JS/bbb", URIUtils.addPaths(";JS", "bbb"));
        assertEquals(";JS/", URIUtils.addPaths(";JS", "/"));
        assertEquals(";JS/bbb", URIUtils.addPaths(";JS", "/bbb"));

        assertEquals("aaa;JS", URIUtils.addPaths("aaa;JS", null));
        assertEquals("aaa;JS", URIUtils.addPaths("aaa;JS", ""));
        assertEquals("aaa;JS/bbb", URIUtils.addPaths("aaa;JS", "bbb"));
        assertEquals("aaa;JS/", URIUtils.addPaths("aaa;JS", "/"));
        assertEquals("aaa;JS/bbb", URIUtils.addPaths("aaa;JS", "/bbb"));

        assertEquals("aaa/;JS", URIUtils.addPaths("aaa/;JS", null));
        assertEquals("aaa/;JS", URIUtils.addPaths("aaa/;JS", ""));
        assertEquals("aaa/;JS/bbb", URIUtils.addPaths("aaa/;JS", "bbb"));
        assertEquals("aaa/;JS/", URIUtils.addPaths("aaa/;JS", "/"));
        assertEquals("aaa/;JS/bbb", URIUtils.addPaths("aaa/;JS", "/bbb"));

        assertEquals("?A=1", URIUtils.addPaths("?A=1", null));
        assertEquals("?A=1", URIUtils.addPaths("?A=1", ""));
        assertEquals("?A=1/bbb", URIUtils.addPaths("?A=1", "bbb"));
        assertEquals("?A=1/", URIUtils.addPaths("?A=1", "/"));
        assertEquals("?A=1/bbb", URIUtils.addPaths("?A=1", "/bbb"));

        assertEquals("aaa?A=1", URIUtils.addPaths("aaa?A=1", null));
        assertEquals("aaa?A=1", URIUtils.addPaths("aaa?A=1", ""));
        assertEquals("aaa?A=1/bbb", URIUtils.addPaths("aaa?A=1", "bbb"));
        assertEquals("aaa?A=1/", URIUtils.addPaths("aaa?A=1", "/"));
        assertEquals("aaa?A=1/bbb", URIUtils.addPaths("aaa?A=1", "/bbb"));

        assertEquals("aaa/?A=1", URIUtils.addPaths("aaa/?A=1", null));
        assertEquals("aaa/?A=1", URIUtils.addPaths("aaa/?A=1", ""));
        assertEquals("aaa/?A=1/bbb", URIUtils.addPaths("aaa/?A=1", "bbb"));
        assertEquals("aaa/?A=1/", URIUtils.addPaths("aaa/?A=1", "/"));
        assertEquals("aaa/?A=1/bbb", URIUtils.addPaths("aaa/?A=1", "/bbb"));
    }


    @Test
    public void testCompactPath() {
        assertEquals("/foo/bar", URIUtils.compactPath("/foo/bar"));
        assertEquals("/foo/bar?a=b//c", URIUtils.compactPath("/foo/bar?a=b//c"));

        assertEquals("/foo/bar", URIUtils.compactPath("//foo//bar"));
        assertEquals("/foo/bar?a=b//c", URIUtils.compactPath("//foo//bar?a=b//c"));

        assertEquals("/foo/bar", URIUtils.compactPath("/foo///bar"));
        assertEquals("/foo/bar?a=b//c", URIUtils.compactPath("/foo///bar?a=b//c"));
    }


    @Test
    public void testParentPath() {
        assertEquals("/aaa/", URIUtils.parentPath("/aaa/bbb/"));
        assertEquals("/aaa/", URIUtils.parentPath("/aaa/bbb"));
        assertEquals("/", URIUtils.parentPath("/aaa/"));
        assertEquals("/", URIUtils.parentPath("/aaa"));
        assertNull(URIUtils.parentPath("/"));
        assertNull(URIUtils.parentPath(null));

    }

    @Test
    public void testEqualsIgnoreEncoding() {
        assertTrue(URIUtils.equalsIgnoreEncodings("http://example.com/foo/bar", "http://example.com/foo/bar"));
        assertTrue(URIUtils.equalsIgnoreEncodings("/barry's", "/barry%27s"));
        assertTrue(URIUtils.equalsIgnoreEncodings("/barry%27s", "/barry's"));
        assertTrue(URIUtils.equalsIgnoreEncodings("/barry%27s", "/barry%27s"));
        assertTrue(URIUtils.equalsIgnoreEncodings("/b rry's", "/b%20rry%27s"));
        assertTrue(URIUtils.equalsIgnoreEncodings("/b rry%27s", "/b%20rry's"));
        assertTrue(URIUtils.equalsIgnoreEncodings("/b rry%27s", "/b%20rry%27s"));

        assertTrue(URIUtils.equalsIgnoreEncodings("/foo%2fbar", "/foo%2fbar"));
        assertTrue(URIUtils.equalsIgnoreEncodings("/foo%2fbar", "/foo%2Fbar"));

        assertFalse(URIUtils.equalsIgnoreEncodings("ABC", "abc"));
        assertFalse(URIUtils.equalsIgnoreEncodings("/barry's", "/barry%26s"));

        assertFalse(URIUtils.equalsIgnoreEncodings("/foo/bar", "/foo%2fbar"));
        assertFalse(URIUtils.equalsIgnoreEncodings("/foo2fbar", "/foo/bar"));
    }

    @Test
    public void testJarSource() throws Exception {
        assertEquals("file:///tmp/", URIUtils.getJarSource("file:///tmp/"));
        assertEquals("file:///tmp/foo.jar", URIUtils.getJarSource("jar:file:///tmp/foo.jar"));
        assertEquals("file:///tmp/foo.jar", URIUtils.getJarSource("jar:file:///tmp/foo.jar!/some/path"));
        assertEquals(new URI("file:///tmp/"), URIUtils.getJarSource(new URI("file:///tmp/")));
        assertEquals(new URI("file:///tmp/foo.jar"), URIUtils.getJarSource(new URI("jar:file:///tmp/foo.jar")));
        assertEquals(new URI("file:///tmp/foo.jar"), URIUtils.getJarSource(new URI("jar:file:///tmp/foo.jar!/some/path")));
    }

}
