package com.fireflysource.net.http.common.codec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Util meta Tests.
 */
@SuppressWarnings("SpellCheckingInspection")
class URIUtilsTest {

    static Stream<Arguments> encodePathSource() {
        return Stream.of(
                Arguments.of("/foo%23+;,:=/b a r/?info ", "/foo%2523+%3B,:=/b%20a%20r/%3Finfo%20"),
                Arguments.of("/context/'list'/\"me\"/;<script>window.alert('xss');</script>",
                        "/context/%27list%27/%22me%22/%3B%3Cscript%3Ewindow.alert(%27xss%27)%3B%3C/script%3E"),
                Arguments.of("test\u00f6?\u00f6:\u00df", "test%C3%B6%3F%C3%B6:%C3%9F"),
                Arguments.of("test?\u00f6?\u00f6:\u00df", "test%3F%C3%B6%3F%C3%B6:%C3%9F")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("encodePathSource")
    void testEncodePath(String rawPath, String expectedEncoded) {
        // test basic encode/decode
        StringBuilder buf = new StringBuilder();
        buf.setLength(0);
        URIUtils.encodePath(buf, rawPath);
        assertEquals(expectedEncoded, buf.toString());
    }

    @Test
    void testEncodeString() {
        StringBuilder buf = new StringBuilder();
        buf.setLength(0);
        URIUtils.encodeString(buf, "foo%23;,:=b a r", ";,= ");
        assertEquals("foo%2523%3b%2c:%3db%20a%20r", buf.toString());
    }

    static Stream<Arguments> decodePathSource() {
        List<Arguments> arguments = new ArrayList<>();
        arguments.add(Arguments.of("/foo/bar", "/foo/bar"));

        arguments.add(Arguments.of("/f%20o/b%20r", "/f o/b r"));
        arguments.add(Arguments.of("fää%2523%3b%2c:%3db%20a%20r%3D", "f\u00e4\u00e4%23;,:=b a r="));
        arguments.add(Arguments.of("f%d8%a9%d8%a9%2523%3b%2c:%3db%20a%20r", "f\u0629\u0629%23;,:=b a r"));

        // path parameters should be ignored
        arguments.add(Arguments.of("/foo;ignore/bar;ignore", "/foo/bar"));
        arguments.add(Arguments.of("/f\u00e4\u00e4;ignore/bar;ignore", "/fää/bar"));
        arguments.add(Arguments.of("/f%d8%a9%d8%a9%2523;ignore/bar;ignore", "/f\u0629\u0629%23/bar"));
        arguments.add(Arguments.of("foo%2523%3b%2c:%3db%20a%20r;rubbish", "foo%23;,:=b a r"));

        // Test for null character (real world ugly test case)
        byte[] oddBytes = {'/', 0x00, '/'};
        String odd = new String(oddBytes, StandardCharsets.ISO_8859_1);
        arguments.add(Arguments.of("/%00/", odd));

        // Deprecated Microsoft Percent-U encoding
        arguments.add(Arguments.of("abc%u3040", "abc\u3040"));

        // Lenient decode
        arguments.add(Arguments.of("abc%xyz", "abc%xyz")); // not a "%##"
        arguments.add(Arguments.of("abc%", "abc%")); // percent at end of string
        arguments.add(Arguments.of("abc%A", "abc%A")); // incomplete "%##" at end of string
        arguments.add(Arguments.of("abc%uvwxyz", "abc%uvwxyz")); // not a valid "%u####"
        arguments.add(Arguments.of("abc%uEFGHIJ", "abc%uEFGHIJ")); // not a valid "%u####"
        arguments.add(Arguments.of("abc%uABC", "abc%uABC")); // incomplete "%u####"
        arguments.add(Arguments.of("abc%uAB", "abc%uAB")); // incomplete "%u####"
        arguments.add(Arguments.of("abc%uA", "abc%uA")); // incomplete "%u####"
        arguments.add(Arguments.of("abc%u", "abc%u")); // incomplete "%u####"

        return arguments.stream();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("decodePathSource")
    void testDecodePath(String encodedPath, String expectedPath) {
        String path = URIUtils.decodePath(encodedPath);
        assertEquals(expectedPath, path);
    }

    @Test
    void testDecodePathSubstring() {
        String path = URIUtils.decodePath("xx/foo/barxx", 2, 8);
        assertEquals("/foo/bar", path);

        path = URIUtils.decodePath("xxx/foo/bar%2523%3b%2c:%3db%20a%20r%3Dxxx;rubbish", 3, 35);
        assertEquals("/foo/bar%23;,:=b a r=", path);
    }

    static Stream<Arguments> addEncodedPathsSource() {
        return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of(null, "", ""),
                Arguments.of(null, "bbb", "bbb"),
                Arguments.of(null, "/", "/"),
                Arguments.of(null, "/bbb", "/bbb"),

                Arguments.of("", null, ""),
                Arguments.of("", "", ""),
                Arguments.of("", "bbb", "bbb"),
                Arguments.of("", "/", "/"),
                Arguments.of("", "/bbb", "/bbb"),

                Arguments.of("aaa", null, "aaa"),
                Arguments.of("aaa", "", "aaa"),
                Arguments.of("aaa", "bbb", "aaa/bbb"),
                Arguments.of("aaa", "/", "aaa/"),
                Arguments.of("aaa", "/bbb", "aaa/bbb"),

                Arguments.of("/", null, "/"),
                Arguments.of("/", "", "/"),
                Arguments.of("/", "bbb", "/bbb"),
                Arguments.of("/", "/", "/"),
                Arguments.of("/", "/bbb", "/bbb"),

                Arguments.of("aaa/", null, "aaa/"),
                Arguments.of("aaa/", "", "aaa/"),
                Arguments.of("aaa/", "bbb", "aaa/bbb"),
                Arguments.of("aaa/", "/", "aaa/"),
                Arguments.of("aaa/", "/bbb", "aaa/bbb"),

                Arguments.of(";JS", null, ";JS"),
                Arguments.of(";JS", "", ";JS"),
                Arguments.of(";JS", "bbb", "bbb;JS"),
                Arguments.of(";JS", "/", "/;JS"),
                Arguments.of(";JS", "/bbb", "/bbb;JS"),

                Arguments.of("aaa;JS", null, "aaa;JS"),
                Arguments.of("aaa;JS", "", "aaa;JS"),
                Arguments.of("aaa;JS", "bbb", "aaa/bbb;JS"),
                Arguments.of("aaa;JS", "/", "aaa/;JS"),
                Arguments.of("aaa;JS", "/bbb", "aaa/bbb;JS"),

                Arguments.of("aaa/;JS", null, "aaa/;JS"),
                Arguments.of("aaa/;JS", "", "aaa/;JS"),
                Arguments.of("aaa/;JS", "bbb", "aaa/bbb;JS"),
                Arguments.of("aaa/;JS", "/", "aaa/;JS"),
                Arguments.of("aaa/;JS", "/bbb", "aaa/bbb;JS"),

                Arguments.of("?A=1", null, "?A=1"),
                Arguments.of("?A=1", "", "?A=1"),
                Arguments.of("?A=1", "bbb", "bbb?A=1"),
                Arguments.of("?A=1", "/", "/?A=1"),
                Arguments.of("?A=1", "/bbb", "/bbb?A=1"),

                Arguments.of("aaa?A=1", null, "aaa?A=1"),
                Arguments.of("aaa?A=1", "", "aaa?A=1"),
                Arguments.of("aaa?A=1", "bbb", "aaa/bbb?A=1"),
                Arguments.of("aaa?A=1", "/", "aaa/?A=1"),
                Arguments.of("aaa?A=1", "/bbb", "aaa/bbb?A=1"),

                Arguments.of("aaa/?A=1", null, "aaa/?A=1"),
                Arguments.of("aaa/?A=1", "", "aaa/?A=1"),
                Arguments.of("aaa/?A=1", "bbb", "aaa/bbb?A=1"),
                Arguments.of("aaa/?A=1", "/", "aaa/?A=1"),
                Arguments.of("aaa/?A=1", "/bbb", "aaa/bbb?A=1"),

                Arguments.of(";JS?A=1", null, ";JS?A=1"),
                Arguments.of(";JS?A=1", "", ";JS?A=1"),
                Arguments.of(";JS?A=1", "bbb", "bbb;JS?A=1"),
                Arguments.of(";JS?A=1", "/", "/;JS?A=1"),
                Arguments.of(";JS?A=1", "/bbb", "/bbb;JS?A=1"),

                Arguments.of("aaa;JS?A=1", null, "aaa;JS?A=1"),
                Arguments.of("aaa;JS?A=1", "", "aaa;JS?A=1"),
                Arguments.of("aaa;JS?A=1", "bbb", "aaa/bbb;JS?A=1"),
                Arguments.of("aaa;JS?A=1", "/", "aaa/;JS?A=1"),
                Arguments.of("aaa;JS?A=1", "/bbb", "aaa/bbb;JS?A=1"),

                Arguments.of("aaa/;JS?A=1", null, "aaa/;JS?A=1"),
                Arguments.of("aaa/;JS?A=1", "", "aaa/;JS?A=1"),
                Arguments.of("aaa/;JS?A=1", "bbb", "aaa/bbb;JS?A=1"),
                Arguments.of("aaa/;JS?A=1", "/", "aaa/;JS?A=1"),
                Arguments.of("aaa/;JS?A=1", "/bbb", "aaa/bbb;JS?A=1")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}+{1}")
    @MethodSource("addEncodedPathsSource")
    void testAddEncodedPaths(String path1, String path2, String expected) {
        String actual = URIUtils.addEncodedPaths(path1, path2);
        assertEquals(expected, actual, String.format("%s+%s", path1, path2));
    }

    static Stream<Arguments> addDecodedPathsSource() {
        return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of(null, "", ""),
                Arguments.of(null, "bbb", "bbb"),
                Arguments.of(null, "/", "/"),
                Arguments.of(null, "/bbb", "/bbb"),

                Arguments.of("", null, ""),
                Arguments.of("", "", ""),
                Arguments.of("", "bbb", "bbb"),
                Arguments.of("", "/", "/"),
                Arguments.of("", "/bbb", "/bbb"),

                Arguments.of("aaa", null, "aaa"),
                Arguments.of("aaa", "", "aaa"),
                Arguments.of("aaa", "bbb", "aaa/bbb"),
                Arguments.of("aaa", "/", "aaa/"),
                Arguments.of("aaa", "/bbb", "aaa/bbb"),

                Arguments.of("/", null, "/"),
                Arguments.of("/", "", "/"),
                Arguments.of("/", "bbb", "/bbb"),
                Arguments.of("/", "/", "/"),
                Arguments.of("/", "/bbb", "/bbb"),

                Arguments.of("aaa/", null, "aaa/"),
                Arguments.of("aaa/", "", "aaa/"),
                Arguments.of("aaa/", "bbb", "aaa/bbb"),
                Arguments.of("aaa/", "/", "aaa/"),
                Arguments.of("aaa/", "/bbb", "aaa/bbb"),

                Arguments.of(";JS", null, ";JS"),
                Arguments.of(";JS", "", ";JS"),
                Arguments.of(";JS", "bbb", ";JS/bbb"),
                Arguments.of(";JS", "/", ";JS/"),
                Arguments.of(";JS", "/bbb", ";JS/bbb"),

                Arguments.of("aaa;JS", null, "aaa;JS"),
                Arguments.of("aaa;JS", "", "aaa;JS"),
                Arguments.of("aaa;JS", "bbb", "aaa;JS/bbb"),
                Arguments.of("aaa;JS", "/", "aaa;JS/"),
                Arguments.of("aaa;JS", "/bbb", "aaa;JS/bbb"),

                Arguments.of("aaa/;JS", null, "aaa/;JS"),
                Arguments.of("aaa/;JS", "", "aaa/;JS"),
                Arguments.of("aaa/;JS", "bbb", "aaa/;JS/bbb"),
                Arguments.of("aaa/;JS", "/", "aaa/;JS/"),
                Arguments.of("aaa/;JS", "/bbb", "aaa/;JS/bbb"),

                Arguments.of("?A=1", null, "?A=1"),
                Arguments.of("?A=1", "", "?A=1"),
                Arguments.of("?A=1", "bbb", "?A=1/bbb"),
                Arguments.of("?A=1", "/", "?A=1/"),
                Arguments.of("?A=1", "/bbb", "?A=1/bbb"),

                Arguments.of("aaa?A=1", null, "aaa?A=1"),
                Arguments.of("aaa?A=1", "", "aaa?A=1"),
                Arguments.of("aaa?A=1", "bbb", "aaa?A=1/bbb"),
                Arguments.of("aaa?A=1", "/", "aaa?A=1/"),
                Arguments.of("aaa?A=1", "/bbb", "aaa?A=1/bbb"),

                Arguments.of("aaa/?A=1", null, "aaa/?A=1"),
                Arguments.of("aaa/?A=1", "", "aaa/?A=1"),
                Arguments.of("aaa/?A=1", "bbb", "aaa/?A=1/bbb"),
                Arguments.of("aaa/?A=1", "/", "aaa/?A=1/"),
                Arguments.of("aaa/?A=1", "/bbb", "aaa/?A=1/bbb")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}+{1}")
    @MethodSource("addDecodedPathsSource")
    void testAddDecodedPaths(String path1, String path2, String expected) {
        String actual = URIUtils.addPaths(path1, path2);
        assertEquals(expected, actual, String.format("%s+%s", path1, path2));
    }

    static Stream<Arguments> compactPathSource() {
        return Stream.of(
                Arguments.of("/foo/bar", "/foo/bar"),
                Arguments.of("/foo/bar?a=b//c", "/foo/bar?a=b//c"),

                Arguments.of("//foo//bar", "/foo/bar"),
                Arguments.of("//foo//bar?a=b//c", "/foo/bar?a=b//c"),

                Arguments.of("/foo///bar", "/foo/bar"),
                Arguments.of("/foo///bar?a=b//c", "/foo/bar?a=b//c")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("compactPathSource")
    void testCompactPath(String path, String expected) {
        String actual = URIUtils.compactPath(path);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> parentPathSource() {
        return Stream.of(
                Arguments.of("/aaa/bbb/", "/aaa/"),
                Arguments.of("/aaa/bbb", "/aaa/"),
                Arguments.of("/aaa/", "/"),
                Arguments.of("/aaa", "/"),
                Arguments.of("/", null),
                Arguments.of(null, null)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("parentPathSource")
    void testParentPath(String path, String expectedPath) {
        String actual = URIUtils.parentPath(path);
        assertEquals(expectedPath, actual, String.format("parent %s", path));
    }

    static Stream<Arguments> equalsIgnoreEncodingStringTrueSource() {
        return Stream.of(
                Arguments.of("http://example.com/foo/bar", "http://example.com/foo/bar"),
                Arguments.of("/barry's", "/barry%27s"),
                Arguments.of("/barry%27s", "/barry's"),
                Arguments.of("/barry%27s", "/barry%27s"),
                Arguments.of("/b rry's", "/b%20rry%27s"),
                Arguments.of("/b rry%27s", "/b%20rry's"),
                Arguments.of("/b rry%27s", "/b%20rry%27s"),

                Arguments.of("/foo%2fbar", "/foo%2fbar"),
                Arguments.of("/foo%2fbar", "/foo%2Fbar"),

                // encoded vs not-encode ("%" symbol is encoded as "%25")
                Arguments.of("/abc%25xyz", "/abc%xyz"),
                Arguments.of("/abc%25xy", "/abc%xy"),
                Arguments.of("/abc%25x", "/abc%x"),
                Arguments.of("/zzz%25", "/zzz%")
        );
    }

    @ParameterizedTest
    @MethodSource("equalsIgnoreEncodingStringTrueSource")
    void testEqualsIgnoreEncodingStringTrue(String uriA, String uriB) {
        assertTrue(URIUtils.equalsIgnoreEncodings(uriA, uriB));
    }

    static Stream<Arguments> equalsIgnoreEncodingStringFalseSource() {
        return Stream.of(
                // case difference
                Arguments.of("ABC", "abc"),
                // Encoding difference ("'" is "%27")
                Arguments.of("/barry's", "/barry%26s"),
                // Never match on "%2f" differences - only intested in filename / directory name differences
                // This could be a directory called "foo" with a file called "bar" on the left, and just a file "foo%2fbar" on the right
                Arguments.of("/foo/bar", "/foo%2fbar"),
                // not actually encoded
                Arguments.of("/foo2fbar", "/foo/bar"),
                // encoded vs not-encode ("%" symbol is encoded as "%25")
                Arguments.of("/yyy%25zzz", "/aaa%xxx"),
                Arguments.of("/zzz%25", "/aaa%")
        );
    }

    @ParameterizedTest
    @MethodSource("equalsIgnoreEncodingStringFalseSource")
    void testEqualsIgnoreEncodingStringFalse(String uriA, String uriB) {
        assertFalse(URIUtils.equalsIgnoreEncodings(uriA, uriB));
    }

    static Stream<Arguments> equalsIgnoreEncodingURITrueSource() {
        return Stream.of(
                Arguments.of(
                        URI.create("jar:file:/path/to/main.jar!/META-INF/versions/"),
                        URI.create("jar:file:/path/to/main.jar!/META-INF/%76ersions/")
                ),
                Arguments.of(
                        URI.create("JAR:FILE:/path/to/main.jar!/META-INF/versions/"),
                        URI.create("jar:file:/path/to/main.jar!/META-INF/versions/")
                )
        );
    }

    @ParameterizedTest
    @MethodSource("equalsIgnoreEncodingURITrueSource")
    void testEqualsIgnoreEncodingURITrue(URI uriA, URI uriB) {
        assertTrue(URIUtils.equalsIgnoreEncodings(uriA, uriB));
    }

    static Stream<Arguments> getJarSourceStringSource() {
        return Stream.of(
                Arguments.of("file:///tmp/", "file:///tmp/"),
                Arguments.of("jar:file:///tmp/foo.jar", "file:///tmp/foo.jar"),
                Arguments.of("jar:file:///tmp/foo.jar!/some/path", "file:///tmp/foo.jar")
        );
    }

    @ParameterizedTest
    @MethodSource("getJarSourceStringSource")
    void testJarSourceString(String uri, String expectedJarUri) {
        assertEquals(expectedJarUri, URIUtils.getJarSource(uri));
    }

    static Stream<Arguments> getJarSourceURISource() {
        return Stream.of(
                Arguments.of(URI.create("file:///tmp/"), URI.create("file:///tmp/")),
                Arguments.of(URI.create("jar:file:///tmp/foo.jar"), URI.create("file:///tmp/foo.jar")),
                Arguments.of(URI.create("jar:file:///tmp/foo.jar!/some/path"), URI.create("file:///tmp/foo.jar"))
        );
    }

    @ParameterizedTest
    @MethodSource("getJarSourceURISource")
    void testJarSourceURI(URI uri, URI expectedJarUri) {
        assertEquals(expectedJarUri, URIUtils.getJarSource(uri));
    }

    static Stream<Arguments> encodeSpacesSource() {
        return Stream.of(
                // null
                Arguments.of(null, null),

                // no spaces
                Arguments.of("abc", "abc"),

                // match
                Arguments.of("a c", "a%20c"),
                Arguments.of("   ", "%20%20%20"),
                Arguments.of("a%20space", "a%20space")
        );
    }

    @ParameterizedTest
    @MethodSource("encodeSpacesSource")
    void testEncodeSpaces(String raw, String expected) {
        assertEquals(expected, URIUtils.encodeSpaces(raw));
    }

    static Stream<Arguments> encodeSpecific() {
        return Stream.of(
                // [raw, chars, expected]

                // null input
                Arguments.of(null, null, null),

                // null chars
                Arguments.of("abc", null, "abc"),

                // empty chars
                Arguments.of("abc", "", "abc"),

                // no matches
                Arguments.of("abc", ".;", "abc"),
                Arguments.of("xyz", ".;", "xyz"),
                Arguments.of(":::", ".;", ":::"),

                // matches
                Arguments.of("a c", " ", "a%20c"),
                Arguments.of("name=value", "=", "name%3Dvalue"),
                Arguments.of("This has fewer then 10% hits.", ".%", "This has fewer then 10%25 hits%2E"),

                // partially encoded already
                Arguments.of("a%20name=value%20pair", "=", "a%20name%3Dvalue%20pair"),
                Arguments.of("a%20name=value%20pair", "=%", "a%2520name%3Dvalue%2520pair")
        );
    }

    @ParameterizedTest
    @MethodSource(value = "encodeSpecific")
    void testEncodeSpecific(String raw, String chars, String expected) {
        assertEquals(expected, URIUtils.encodeSpecific(raw, chars));
    }

    static Stream<Arguments> decodeSpecific() {
        return Stream.of(
                // [raw, chars, expected]

                // null input
                Arguments.of(null, null, null),

                // null chars
                Arguments.of("abc", null, "abc"),

                // empty chars
                Arguments.of("abc", "", "abc"),

                // no matches
                Arguments.of("abc", ".;", "abc"),
                Arguments.of("xyz", ".;", "xyz"),
                Arguments.of(":::", ".;", ":::"),

                // matches
                Arguments.of("a%20c", " ", "a c"),
                Arguments.of("name%3Dvalue", "=", "name=value"),
                Arguments.of("This has fewer then 10%25 hits%2E", ".%", "This has fewer then 10% hits."),

                // partially decode
                Arguments.of("a%20name%3Dvalue%20pair", "=", "a%20name=value%20pair"),
                Arguments.of("a%2520name%3Dvalue%2520pair", "=%", "a%20name=value%20pair")
        );
    }

    @ParameterizedTest
    @MethodSource(value = "decodeSpecific")
    void testDecodeSpecific(String raw, String chars, String expected) {
        assertEquals(expected, URIUtils.decodeSpecific(raw, chars));
    }

}
