package test.utils.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.firefly.utils.lang.URIUtils;

/**
 * Util meta Tests.
 *
 */
public class URIUtilsTest {

	@Test
	public void testEncodePath() {
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
		assertEquals("/context/%27list%27/%22me%22/%3B%3Cscript%3Ewindow.alert(%27xss%27)%3B%3C/script%3E",
				buf.toString());
	}

	@Test
	public void testDecodePath() {
		assertEquals("/foo/bar", URIUtils.decodePath("xx/foo/barxx", 2, 8));
		assertEquals("/foo/bar", URIUtils.decodePath("/foo/bar"));
		assertEquals("/f o/b r", URIUtils.decodePath("/f%20o/b%20r"));
		assertEquals("/foo/bar", URIUtils.decodePath("/foo;ignore/bar;ignore"));
		assertEquals("/fää/bar", URIUtils.decodePath("/fää;ignore/bar;ignore"));
		assertEquals("/f\u0629\u0629%23/bar", URIUtils.decodePath("/f%d8%a9%d8%a9%2523;ignore/bar;ignore"));

		assertEquals("foo%23;,:=b a r", URIUtils.decodePath("foo%2523%3b%2c:%3db%20a%20r;rubbish"));
		assertEquals("/foo/bar%23;,:=b a r=",
				URIUtils.decodePath("xxx/foo/bar%2523%3b%2c:%3db%20a%20r%3Dxxx;rubbish", 3, 35));
		assertEquals("fää%23;,:=b a r=", URIUtils.decodePath("fää%2523%3b%2c:%3db%20a%20r%3D"));
		assertEquals("f\u0629\u0629%23;,:=b a r", URIUtils.decodePath("f%d8%a9%d8%a9%2523%3b%2c:%3db%20a%20r"));

		// Test for null character (real world ugly test case)
		byte oddBytes[] = { '/', 0x00, '/' };
		String odd = new String(oddBytes, StandardCharsets.ISO_8859_1);
		assertEquals(odd, URIUtils.decodePath("/%00/"));
	}

	@Test
	public void testAddPaths() {
		assertEquals("null+null", URIUtils.addPaths(null, null), null);
		assertEquals("null+", URIUtils.addPaths(null, ""), "");
		assertEquals("null+bbb", URIUtils.addPaths(null, "bbb"), "bbb");
		assertEquals("null+/", URIUtils.addPaths(null, "/"), "/");
		assertEquals("null+/bbb", URIUtils.addPaths(null, "/bbb"), "/bbb");

		assertEquals("+null", URIUtils.addPaths("", null), "");
		assertEquals("+", URIUtils.addPaths("", ""), "");
		assertEquals("+bbb", URIUtils.addPaths("", "bbb"), "bbb");
		assertEquals("+/", URIUtils.addPaths("", "/"), "/");
		assertEquals("+/bbb", URIUtils.addPaths("", "/bbb"), "/bbb");

		assertEquals("aaa+null", URIUtils.addPaths("aaa", null), "aaa");
		assertEquals("aaa+", URIUtils.addPaths("aaa", ""), "aaa");
		assertEquals("aaa+bbb", URIUtils.addPaths("aaa", "bbb"), "aaa/bbb");
		assertEquals("aaa+/", URIUtils.addPaths("aaa", "/"), "aaa/");
		assertEquals("aaa+/bbb", URIUtils.addPaths("aaa", "/bbb"), "aaa/bbb");

		assertEquals("/+null", URIUtils.addPaths("/", null), "/");
		assertEquals("/+", URIUtils.addPaths("/", ""), "/");
		assertEquals("/+bbb", URIUtils.addPaths("/", "bbb"), "/bbb");
		assertEquals("/+/", URIUtils.addPaths("/", "/"), "/");
		assertEquals("/+/bbb", URIUtils.addPaths("/", "/bbb"), "/bbb");

		assertEquals("aaa/+null", URIUtils.addPaths("aaa/", null), "aaa/");
		assertEquals("aaa/+", URIUtils.addPaths("aaa/", ""), "aaa/");
		assertEquals("aaa/+bbb", URIUtils.addPaths("aaa/", "bbb"), "aaa/bbb");
		assertEquals("aaa/+/", URIUtils.addPaths("aaa/", "/"), "aaa/");
		assertEquals("aaa/+/bbb", URIUtils.addPaths("aaa/", "/bbb"), "aaa/bbb");

		assertEquals(";JS+null", URIUtils.addPaths(";JS", null), ";JS");
		assertEquals(";JS+", URIUtils.addPaths(";JS", ""), ";JS");
		assertEquals(";JS+bbb", URIUtils.addPaths(";JS", "bbb"), "bbb;JS");
		assertEquals(";JS+/", URIUtils.addPaths(";JS", "/"), "/;JS");
		assertEquals(";JS+/bbb", URIUtils.addPaths(";JS", "/bbb"), "/bbb;JS");

		assertEquals("aaa;JS+null", URIUtils.addPaths("aaa;JS", null), "aaa;JS");
		assertEquals("aaa;JS+", URIUtils.addPaths("aaa;JS", ""), "aaa;JS");
		assertEquals("aaa;JS+bbb", URIUtils.addPaths("aaa;JS", "bbb"), "aaa/bbb;JS");
		assertEquals("aaa;JS+/", URIUtils.addPaths("aaa;JS", "/"), "aaa/;JS");
		assertEquals("aaa;JS+/bbb", URIUtils.addPaths("aaa;JS", "/bbb"), "aaa/bbb;JS");

		assertEquals("aaa;JS+null", URIUtils.addPaths("aaa/;JS", null), "aaa/;JS");
		assertEquals("aaa;JS+", URIUtils.addPaths("aaa/;JS", ""), "aaa/;JS");
		assertEquals("aaa;JS+bbb", URIUtils.addPaths("aaa/;JS", "bbb"), "aaa/bbb;JS");
		assertEquals("aaa;JS+/", URIUtils.addPaths("aaa/;JS", "/"), "aaa/;JS");
		assertEquals("aaa;JS+/bbb", URIUtils.addPaths("aaa/;JS", "/bbb"), "aaa/bbb;JS");

		assertEquals("?A=1+null", URIUtils.addPaths("?A=1", null), "?A=1");
		assertEquals("?A=1+", URIUtils.addPaths("?A=1", ""), "?A=1");
		assertEquals("?A=1+bbb", URIUtils.addPaths("?A=1", "bbb"), "bbb?A=1");
		assertEquals("?A=1+/", URIUtils.addPaths("?A=1", "/"), "/?A=1");
		assertEquals("?A=1+/bbb", URIUtils.addPaths("?A=1", "/bbb"), "/bbb?A=1");

		assertEquals("aaa?A=1+null", URIUtils.addPaths("aaa?A=1", null), "aaa?A=1");
		assertEquals("aaa?A=1+", URIUtils.addPaths("aaa?A=1", ""), "aaa?A=1");
		assertEquals("aaa?A=1+bbb", URIUtils.addPaths("aaa?A=1", "bbb"), "aaa/bbb?A=1");
		assertEquals("aaa?A=1+/", URIUtils.addPaths("aaa?A=1", "/"), "aaa/?A=1");
		assertEquals("aaa?A=1+/bbb", URIUtils.addPaths("aaa?A=1", "/bbb"), "aaa/bbb?A=1");

		assertEquals("aaa?A=1+null", URIUtils.addPaths("aaa/?A=1", null), "aaa/?A=1");
		assertEquals("aaa?A=1+", URIUtils.addPaths("aaa/?A=1", ""), "aaa/?A=1");
		assertEquals("aaa?A=1+bbb", URIUtils.addPaths("aaa/?A=1", "bbb"), "aaa/bbb?A=1");
		assertEquals("aaa?A=1+/", URIUtils.addPaths("aaa/?A=1", "/"), "aaa/?A=1");
		assertEquals("aaa?A=1+/bbb", URIUtils.addPaths("aaa/?A=1", "/bbb"), "aaa/bbb?A=1");

		assertEquals(";JS?A=1+null", URIUtils.addPaths(";JS?A=1", null), ";JS?A=1");
		assertEquals(";JS?A=1+", URIUtils.addPaths(";JS?A=1", ""), ";JS?A=1");
		assertEquals(";JS?A=1+bbb", URIUtils.addPaths(";JS?A=1", "bbb"), "bbb;JS?A=1");
		assertEquals(";JS?A=1+/", URIUtils.addPaths(";JS?A=1", "/"), "/;JS?A=1");
		assertEquals(";JS?A=1+/bbb", URIUtils.addPaths(";JS?A=1", "/bbb"), "/bbb;JS?A=1");

		assertEquals("aaa;JS?A=1+null", URIUtils.addPaths("aaa;JS?A=1", null), "aaa;JS?A=1");
		assertEquals("aaa;JS?A=1+", URIUtils.addPaths("aaa;JS?A=1", ""), "aaa;JS?A=1");
		assertEquals("aaa;JS?A=1+bbb", URIUtils.addPaths("aaa;JS?A=1", "bbb"), "aaa/bbb;JS?A=1");
		assertEquals("aaa;JS?A=1+/", URIUtils.addPaths("aaa;JS?A=1", "/"), "aaa/;JS?A=1");
		assertEquals("aaa;JS?A=1+/bbb", URIUtils.addPaths("aaa;JS?A=1", "/bbb"), "aaa/bbb;JS?A=1");

		assertEquals("aaa;JS?A=1+null", URIUtils.addPaths("aaa/;JS?A=1", null), "aaa/;JS?A=1");
		assertEquals("aaa;JS?A=1+", URIUtils.addPaths("aaa/;JS?A=1", ""), "aaa/;JS?A=1");
		assertEquals("aaa;JS?A=1+bbb", URIUtils.addPaths("aaa/;JS?A=1", "bbb"), "aaa/bbb;JS?A=1");
		assertEquals("aaa;JS?A=1+/", URIUtils.addPaths("aaa/;JS?A=1", "/"), "aaa/;JS?A=1");
		assertEquals("aaa;JS?A=1+/bbb", URIUtils.addPaths("aaa/;JS?A=1", "/bbb"), "aaa/bbb;JS?A=1");

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
		assertEquals("parent /aaa/bbb/", "/aaa/", URIUtils.parentPath("/aaa/bbb/"));
		assertEquals("parent /aaa/bbb", "/aaa/", URIUtils.parentPath("/aaa/bbb"));
		assertEquals("parent /aaa/", "/", URIUtils.parentPath("/aaa/"));
		assertEquals("parent /aaa", "/", URIUtils.parentPath("/aaa"));
		assertEquals("parent /", null, URIUtils.parentPath("/"));
		assertEquals("parent null", null, URIUtils.parentPath(null));

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
	public void testCanonicalPath() {
		String[][] canonical = { { "/aaa/bbb/", "/aaa/bbb/" }, { "/aaa//bbb/", "/aaa//bbb/" },
				{ "/aaa///bbb/", "/aaa///bbb/" }, { "/aaa/./bbb/", "/aaa/bbb/" }, { "/aaa/../bbb/", "/bbb/" },
				{ "/aaa/./../bbb/", "/bbb/" }, { "/aaa/bbb/ccc/../../ddd/", "/aaa/ddd/" }, { "./bbb/", "bbb/" },
				{ "./aaa/../bbb/", "bbb/" }, { "./", "" }, { ".//", ".//" }, { ".///", ".///" }, { "/.", "/" },
				{ "//.", "//" }, { "///.", "///" }, { "/", "/" }, { "aaa/bbb", "aaa/bbb" }, { "aaa/", "aaa/" },
				{ "aaa", "aaa" }, { "/aaa/bbb", "/aaa/bbb" }, { "/aaa//bbb", "/aaa//bbb" },
				{ "/aaa/./bbb", "/aaa/bbb" }, { "/aaa/../bbb", "/bbb" }, { "/aaa/./../bbb", "/bbb" },
				{ "./bbb", "bbb" }, { "./aaa/../bbb", "bbb" }, { "aaa/bbb/..", "aaa/" }, { "aaa/bbb/../", "aaa/" },
				{ "/aaa//../bbb", "/aaa/bbb" }, { "/aaa/./../bbb", "/bbb" }, { "./", "" }, { ".", "" }, { "", "" },
				{ "..", null }, { "./..", null }, { "aaa/../..", null }, { "/foo/bar/../../..", null },
				{ "/../foo", null }, { "/foo/.", "/foo/" }, { "a", "a" }, { "a/", "a/" }, { "a/.", "a/" },
				{ "a/..", "" }, { "a/../..", null }, { "/foo/../bar//", "/bar//" }, };

		for (int t = 0; t < canonical.length; t++)
			assertEquals("canonical " + canonical[t][0], canonical[t][1], URIUtils.canonicalPath(canonical[t][0]));

	}

}
