package com.fireflysource.net.http.common.codec;

import com.fireflysource.net.http.model.HttpURI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class HttpURIParseTest {

    public static Stream<Arguments> testParametersProvider() {
        return data().stream().map(arr -> arguments(Arrays.asList(arr).toArray()));
    }

    public static List<String[]> data() {
        String[][] tests = {
                // Nothing but path
                {"path", null, null, "-1", "path", null, null, null},
                {"path/path", null, null, "-1", "path/path", null, null, null},
                {"%65ncoded/path", null, null, "-1", "%65ncoded/path", null, null, null},

                // Basic path reference
                {"/path/to/context", null, null, "-1", "/path/to/context", null, null, null},

                // Basic with encoded query
                {"http://example.com/path/to/context;param?query=%22value%22#fragment", "http", "example.com", "-1",
                        "/path/to/context;param", "param", "query=%22value%22", "fragment"},
                {"http://[::1]/path/to/context;param?query=%22value%22#fragment", "http", "[::1]", "-1",
                        "/path/to/context;param", "param", "query=%22value%22", "fragment"},

                // Basic with parameters and query
                {"http://example.com:8080/path/to/context;param?query=%22value%22#fragment", "http", "example.com",
                        "8080", "/path/to/context;param", "param", "query=%22value%22", "fragment"},
                {"http://[::1]:8080/path/to/context;param?query=%22value%22#fragment", "http", "[::1]", "8080",
                        "/path/to/context;param", "param", "query=%22value%22", "fragment"},

                // Path References
                {"/path/info", null, null, null, "/path/info", null, null, null},
                {"/path/info#fragment", null, null, null, "/path/info", null, null, "fragment"},
                {"/path/info?query", null, null, null, "/path/info", null, "query", null},
                {"/path/info?query#fragment", null, null, null, "/path/info", null, "query", "fragment"},
                {"/path/info;param", null, null, null, "/path/info;param", "param", null, null},
                {"/path/info;param#fragment", null, null, null, "/path/info;param", "param", null, "fragment"},
                {"/path/info;param?query", null, null, null, "/path/info;param", "param", "query", null},
                {"/path/info;param?query#fragment", null, null, null, "/path/info;param", "param", "query",
                        "fragment"},

                // Protocol Less (aka scheme-less) URIs
                {"//host/path/info", null, "host", null, "/path/info", null, null, null},
                {"//user@host/path/info", null, "host", null, "/path/info", null, null, null},
                {"//user@host:8080/path/info", null, "host", "8080", "/path/info", null, null, null},
                {"//host:8080/path/info", null, "host", "8080", "/path/info", null, null, null},

                // Host Less
                {"http:/path/info", "http", null, null, "/path/info", null, null, null},
                {"http:/path/info#fragment", "http", null, null, "/path/info", null, null, "fragment"},
                {"http:/path/info?query", "http", null, null, "/path/info", null, "query", null},
                {"http:/path/info?query#fragment", "http", null, null, "/path/info", null, "query", "fragment"},
                {"http:/path/info;param", "http", null, null, "/path/info;param", "param", null, null},
                {"http:/path/info;param#fragment", "http", null, null, "/path/info;param", "param", null, "fragment"},
                {"http:/path/info;param?query", "http", null, null, "/path/info;param", "param", "query", null},
                {"http:/path/info;param?query#fragment", "http", null, null, "/path/info;param", "param", "query",
                        "fragment"},

                // Everything and the kitchen sink
                {"http://user@host:8080/path/info;param?query#fragment", "http", "host", "8080", "/path/info;param",
                        "param", "query", "fragment"},
                {"xxxxx://user@host:8080/path/info;param?query#fragment", "xxxxx", "host", "8080", "/path/info;param",
                        "param", "query", "fragment"},

                // No host, parameter with no content
                {"http:///;?#", "http", null, null, "/;", "", "", ""},

                // Path with query that has no value
                {"/path/info?a=?query", null, null, null, "/path/info", null, "a=?query", null},

                // Path with query alt syntax
                {"/path/info?a=;query", null, null, null, "/path/info", null, "a=;query", null},

                // URI with host character
                {"/@path/info", null, null, null, "/@path/info", null, null, null},
                {"/user@path/info", null, null, null, "/user@path/info", null, null, null},
                {"//user@host/info", null, "host", null, "/info", null, null, null},
                {"//@host/info", null, "host", null, "/info", null, null, null},
                {"@host/info", null, null, null, "@host/info", null, null, null},

                // Scheme-less, with host and port (overlapping with path)
                {"//host:8080//", null, "host", "8080", "//", null, null, null},

                // File reference
                {"file:///path/info", "file", null, null, "/path/info", null, null, null},
                {"file:/path/info", "file", null, null, "/path/info", null, null, null},

                // Bad URI (no scheme, no host, no path)
                {"//", null, null, null, null, null, null, null},

                // Simple localhost references
                {"http://localhost/", "http", "localhost", null, "/", null, null, null},
                {"http://localhost:8080/", "http", "localhost", "8080", "/", null, null, null},
                {"http://localhost/?x=y", "http", "localhost", null, "/", null, "x=y", null},

                // Simple path with parameter
                {"/;param", null, null, null, "/;param", "param", null, null},
                {";param", null, null, null, ";param", "param", null, null},

                // Simple path with query
                {"/?x=y", null, null, null, "/", null, "x=y", null},
                {"/?abc=test", null, null, null, "/", null, "abc=test", null},

                // Simple path with fragment
                {"/#fragment", null, null, null, "/", null, null, "fragment"},

                // Simple IPv4 host with port (default path)
                {"http://192.0.0.1:8080/", "http", "192.0.0.1", "8080", "/", null, null, null},

                // Simple IPv6 host with port (default path)

                {"http://[2001:db8::1]:8080/", "http", "[2001:db8::1]", "8080", "/", null, null, null},
                // IPv6 authenticated host with port (default path)

                {"http://user@[2001:db8::1]:8080/", "http", "[2001:db8::1]", "8080", "/", null, null, null},

                // Simple IPv6 host no port (default path)
                {"http://[2001:db8::1]/", "http", "[2001:db8::1]", null, "/", null, null, null},

                // Scheme-less IPv6, host with port (default path)
                {"//[2001:db8::1]:8080/", null, "[2001:db8::1]", "8080", "/", null, null, null},

                // Interpreted as relative path of "*" (no
                // host/port/scheme/query/fragment)
                {"*", null, null, null, "*", null, null, null},

                // Path detection Tests (seen from JSP/JSTL and <c:url> use
                {"http://host:8080/path/info?q1=v1&q2=v2", "http", "host", "8080", "/path/info", null, "q1=v1&q2=v2",
                        null},
                {"/path/info?q1=v1&q2=v2", null, null, null, "/path/info", null, "q1=v1&q2=v2", null},
                {"/info?q1=v1&q2=v2", null, null, null, "/info", null, "q1=v1&q2=v2", null},
                {"info?q1=v1&q2=v2", null, null, null, "info", null, "q1=v1&q2=v2", null},
                {"info;q1=v1?q2=v2", null, null, null, "info;q1=v1", "q1=v1", "q2=v2", null},

                // Path-less, query only (seen from JSP/JSTL and <c:url> use
                {"?q1=v1&q2=v2", null, null, null, "", null, "q1=v1&q2=v2", null}
        };

        return Arrays.asList(tests);
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    void testParseString(String input, String scheme, String host, String port, String path, String param, String query, String fragment) {
        HttpURI httpUri = new HttpURI(input);

        try {
            new URI(input);
            // URI is valid (per java.net.URI parsing)

            // Test case sanity check
            assertNotNull(path);

            // Assert expectations
            assertEquals(host, httpUri.getHost());
            assertEquals(port == null ? -1 : Integer.parseInt(port), httpUri.getPort());
            assertEquals(path, httpUri.getPath());
            assertEquals(param, httpUri.getParam());
            assertEquals(query, httpUri.getQuery());
            assertEquals(fragment, httpUri.getFragment());
            assertEquals(input, httpUri.toString());
            assertEquals(scheme, httpUri.getScheme());
        } catch (URISyntaxException e) {
            // Assert HttpURI values for invalid URI (such as "//")
            assertNull(httpUri.getScheme());
            assertNull(httpUri.getHost());
            assertEquals(-1, httpUri.getPort());
            assertNull(httpUri.getPath());
            assertNull(httpUri.getParam());
            assertNull(httpUri.getQuery());
            assertNull(httpUri.getFragment());
        }
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    void testParseURI(String input, String scheme, String host, String port, String path, String param, String query, String fragment) {
        URI javaUri = null;
        try {
            javaUri = new URI(input);
        } catch (URISyntaxException ignore) {
        }

        assumeTrue(javaUri != null);
        HttpURI httpUri = new HttpURI(javaUri);

        assertEquals(scheme, httpUri.getScheme());
        assertEquals(host, httpUri.getHost());
        assertEquals(port == null ? -1 : Integer.parseInt(port), httpUri.getPort());
        assertEquals(path, httpUri.getPath());
        assertEquals(param, httpUri.getParam());
        assertEquals(query, httpUri.getQuery());
        assertEquals(fragment, httpUri.getFragment());
        assertEquals(input, httpUri.toString());
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    void testCompareToJavaNetURI(String input, String scheme, String host, String port, String path, String param, String query, String fragment) {
        URI javaUri = null;
        try {
            javaUri = new URI(input);
        } catch (URISyntaxException ignore) {
        }

        assumeTrue(javaUri != null);
        HttpURI httpUri = new HttpURI(javaUri);

        assertEquals(javaUri.getScheme(), httpUri.getScheme());
        assertEquals(javaUri.getHost(), httpUri.getHost());
        assertEquals(javaUri.getPort(), httpUri.getPort());
        assertEquals(javaUri.getRawPath(), httpUri.getPath());
        assertEquals(javaUri.getRawQuery(), httpUri.getQuery());
        assertEquals(javaUri.getFragment(), httpUri.getFragment());
        assertEquals(javaUri.toASCIIString(), httpUri.toString());
    }
}