package com.fireflysource.net.http.v1.decoder;


import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.exception.BadMessageException;
import com.fireflysource.net.http.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fireflysource.net.http.model.HttpComplianceSection.NO_FIELD_FOLDING;
import static com.fireflysource.net.http.v1.decoder.HttpParser.State;
import static org.junit.jupiter.api.Assertions.*;

class HttpParserTest {
    static {
        HttpCompliance.CUSTOM0.sections().remove(HttpComplianceSection.NO_WS_AFTER_FIELD_NAME);
    }

    private final List<HttpComplianceSection> complianceViolation = new ArrayList<>();
    private String host;
    private int port;
    private String bad;
    private String content;
    private String methodOrVersion;
    private String uriOrStatus;
    private String versionOrReason;
    private List<HttpField> fields = new ArrayList<>();
    private List<HttpField> trailers = new ArrayList<>();
    private String[] hdr;
    private String[] val;
    private int headers;
    private boolean early;
    private boolean headerCompleted;
    private boolean messageCompleted;

    /**
     * Parse until {@link State#END} state.
     * If the parser is already in the END state, then it is {@link HttpParser#reset()} and re-parsed.
     *
     * @param parser The parser to test
     * @param buffer the buffer to parse
     * @throws IllegalStateException If the buffers have already been partially parsed.
     */
    static void parseAll(HttpParser parser, ByteBuffer buffer) {
        if (parser.isState(State.END))
            parser.reset();
        if (!parser.isState(State.START))
            throw new IllegalStateException("!START");

        // continue parsing
        int remaining = buffer.remaining();
        while (!parser.isState(State.END) && remaining > 0) {
            int was_remaining = remaining;
            parser.parseNext(buffer);
            remaining = buffer.remaining();
            if (remaining == was_remaining)
                break;
        }
    }

    @Test
    void HttpMethodTest() {
        assertNull(HttpMethod.lookAheadGet(BufferUtils.toBuffer("Wibble ")));
        assertNull(HttpMethod.lookAheadGet(BufferUtils.toBuffer("GET")));
        assertNull(HttpMethod.lookAheadGet(BufferUtils.toBuffer("MO")));

        assertEquals(HttpMethod.GET, HttpMethod.lookAheadGet(BufferUtils.toBuffer("GET ")));
        assertEquals(HttpMethod.MOVE, HttpMethod.lookAheadGet(BufferUtils.toBuffer("MOVE ")));

        ByteBuffer b = BufferUtils.allocateDirect(128);
        BufferUtils.append(b, BufferUtils.toBuffer("GET"));
        assertNull(HttpMethod.lookAheadGet(b));

        BufferUtils.append(b, BufferUtils.toBuffer(" "));
        assertEquals(HttpMethod.GET, HttpMethod.lookAheadGet(b));
    }

    @Test
    void testLineParse_Mock_IP() {
        ByteBuffer buffer = BufferUtils.toBuffer("POST /mock/127.0.0.1 HTTP/1.1\r\n" + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("POST", methodOrVersion);
        assertEquals("/mock/127.0.0.1", uriOrStatus);
        assertEquals("HTTP/1.1", versionOrReason);
        assertEquals(-1, headers);
    }

    @Test
    void testLineParse0() {
        ByteBuffer buffer = BufferUtils.toBuffer("POST /foo HTTP/1.0\r\n" + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("POST", methodOrVersion);
        assertEquals("/foo", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(-1, headers);
    }

    @Test
    void testLineParse1_RFC2616() {
        ByteBuffer buffer = BufferUtils.toBuffer("GET /999\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, HttpCompliance.RFC2616_LEGACY);
        parseAll(parser, buffer);

        assertNull(bad);
        assertEquals("GET", methodOrVersion);
        assertEquals("/999", uriOrStatus);
        assertEquals("HTTP/0.9", versionOrReason);
        assertEquals(-1, headers);
        assertTrue(complianceViolation.contains(HttpComplianceSection.NO_HTTP_0_9));
    }

    @Test
    void testLineParse1() {
        ByteBuffer buffer = BufferUtils.toBuffer("GET /999\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("HTTP/0.9 not supported", bad);
        assertTrue(complianceViolation.isEmpty());
    }

    @Test
    void testLineParse2_RFC2616() {
        ByteBuffer buffer = BufferUtils.toBuffer("POST /222  \r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, HttpCompliance.RFC2616_LEGACY);
        parseAll(parser, buffer);

        assertNull(bad);
        assertEquals("POST", methodOrVersion);
        assertEquals("/222", uriOrStatus);
        assertEquals("HTTP/0.9", versionOrReason);
        assertEquals(-1, headers);
        assertTrue(complianceViolation.contains(HttpComplianceSection.NO_HTTP_0_9));
    }

    @Test
    void testLineParse2() {
        ByteBuffer buffer = BufferUtils.toBuffer("POST /222  \r\n");

        versionOrReason = null;
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("HTTP/0.9 not supported", bad);
        assertTrue(complianceViolation.isEmpty());
    }

    @Test
    void testLineParse3() {
        ByteBuffer buffer = BufferUtils.toBuffer("POST /fo\u0690 HTTP/1.0\r\n" + "\r\n", StandardCharsets.UTF_8);

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("POST", methodOrVersion);
        assertEquals("/fo\u0690", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(-1, headers);
    }

    @Test
    void testLineParse4() {
        ByteBuffer buffer = BufferUtils.toBuffer("POST /foo?param=\u0690 HTTP/1.0\r\n" + "\r\n", StandardCharsets.UTF_8);

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("POST", methodOrVersion);
        assertEquals("/foo?param=\u0690", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(-1, headers);
    }

    @Test
    void testLongURLParse() {
        ByteBuffer buffer = BufferUtils.toBuffer("POST /123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/ HTTP/1.0\r\n" + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("POST", methodOrVersion);
        assertEquals("/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/123456789abcdef/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(-1, headers);
    }

    @Test
    void testAllowedLinePreamble() {
        ByteBuffer buffer = BufferUtils.toBuffer("\r\n\r\nGET / HTTP/1.0\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(-1, headers);
    }

    @Test
    void testDisallowedLinePreamble() {
        ByteBuffer buffer = BufferUtils.toBuffer("\r\n \r\nGET / HTTP/1.0\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("Illegal character SPACE=' '", bad);
    }

    @Test
    void testConnect() {
        ByteBuffer buffer = BufferUtils.toBuffer("CONNECT 192.168.1.2:80 HTTP/1.1\r\n" + "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("CONNECT", methodOrVersion);
        assertEquals("192.168.1.2:80", uriOrStatus);
        assertEquals("HTTP/1.1", versionOrReason);
        assertEquals(-1, headers);
    }

    @Test
    void testSimple() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("Host", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals("Connection", hdr[1]);
        assertEquals("close", val[1]);
        assertEquals(1, headers);
    }

    @Test
    void testFoldedField2616() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Name: value\r\n" +
                        " extra\r\n" +
                        "Name2: \r\n" +
                        "\tvalue2\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, HttpCompliance.RFC2616_LEGACY);
        parseAll(parser, buffer);

        assertNull(bad);
        assertEquals("Host", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals(2, headers);
        assertEquals("Name", hdr[1]);
        assertEquals("value extra", val[1]);
        assertEquals("Name2", hdr[2]);
        assertEquals("value2", val[2]);
        Arrays.asList(NO_FIELD_FOLDING, NO_FIELD_FOLDING)
              .forEach(e -> assertTrue(complianceViolation.contains(e)));
    }

    @Test
    void testFoldedField7230() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Name: value\r\n" +
                        " extra\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, 4096, HttpCompliance.RFC7230_LEGACY);
        parseAll(parser, buffer);

        assertNotNull(bad);
        assertTrue(bad.contains("Header Folding"));
        assertTrue(complianceViolation.isEmpty());
    }

    @Test
    void testWhiteSpaceInName() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "N ame: value\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, 4096, HttpCompliance.RFC7230_LEGACY);
        parseAll(parser, buffer);

        assertNotNull(bad);
        assertTrue(bad.contains("Illegal character"));
    }

    @Test
    void testWhiteSpaceAfterName() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Name : value\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, 4096, HttpCompliance.RFC7230_LEGACY);
        parseAll(parser, buffer);

        assertNotNull(bad);
        assertTrue(bad.contains("Illegal character"));
    }

    @Test
        // TODO: Parameterize Test
    void testWhiteSpaceBeforeRequest() {
        HttpCompliance[] complianceArray = {
                HttpCompliance.RFC7230, HttpCompliance.RFC2616
        };

        String[][] whitespaces = {
                {" ", "Illegal character SPACE"},
                {"\t", "Illegal character HTAB"},
                {"\n", null},
                {"\r", "Bad EOL"},
                {"\r\n", null},
                {"\r\n\r\n", null},
                {"\r\n \r\n", "Illegal character SPACE"},
                {"\r\n\t\r\n", "Illegal character HTAB"},
                {"\r\t\n", "Bad EOL"},
                {"\r\r\n", "Bad EOL"},
                {"\t\r\t\r\n", "Illegal character HTAB"},
                {" \t \r \t \n\n", "Illegal character SPACE"},
                {" \r \t \r\n\r\n\r\n", "Illegal character SPACE"}
        };


        for (HttpCompliance compliance : complianceArray) {
            for (int j = 0; j < whitespaces.length; j++) {
                String request =
                        whitespaces[j][0] +
                                "GET / HTTP/1.1\r\n" +
                                "Host: localhost\r\n" +
                                "Name: value" + j + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n";

                ByteBuffer buffer = BufferUtils.toBuffer(request);
                HttpParser.RequestHandler handler = new Handler();
                HttpParser parser = new HttpParser(handler, 4096, compliance);
                bad = null;
                parseAll(parser, buffer);

                String test = "whitespace.[" + compliance + "].[" + j + "]";
                String expected = whitespaces[j][1];
                if (expected == null)
                    assertNull(bad, test);
                else
                    assertTrue(bad.contains(expected), test);
            }
        }
    }

    @Test
    void testNoValue() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Name0: \r\n" +
                        "Name1:\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("Host", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals("Name0", hdr[1]);
        assertEquals("", val[1]);
        assertEquals("Name1", hdr[2]);
        assertEquals("", val[2]);
        assertEquals(2, headers);
    }

    @Test
    void testSpaceInNameCustom0() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Name with space: value\r\n" +
                        "Other: value\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, HttpCompliance.CUSTOM0);
        parseAll(parser, buffer);

        assertTrue(bad.contains("Illegal character"));
        assertTrue(complianceViolation.contains(HttpComplianceSection.NO_WS_AFTER_FIELD_NAME));
    }

    @Test
    void testNoColonCustom0() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Name \r\n" +
                        "Other: value\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, HttpCompliance.CUSTOM0);
        parseAll(parser, buffer);

        assertTrue(bad.contains("Illegal character"));
        assertTrue(complianceViolation.contains(HttpComplianceSection.NO_WS_AFTER_FIELD_NAME));
    }

    @Test
    void testTrailingSpacesInHeaderNameInCustom0Mode() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 204 No Content\r\n" +
                        "Access-Control-Allow-Headers : Origin\r\n" +
                        "Other\t : value\r\n" +
                        "\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, -1, HttpCompliance.CUSTOM0);
        parseAll(parser, buffer);

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);

        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("204", uriOrStatus);
        assertEquals("No Content", versionOrReason);
        assertNull(content);

        assertEquals(1, headers);
        System.out.println(Arrays.asList(hdr));
        System.out.println(Arrays.asList(val));
        assertEquals("Access-Control-Allow-Headers", hdr[0]);
        assertEquals("Origin", val[0]);
        assertEquals("Other", hdr[1]);
        assertEquals("value", val[1]);

        Arrays.asList(HttpComplianceSection.NO_WS_AFTER_FIELD_NAME, HttpComplianceSection.NO_WS_AFTER_FIELD_NAME)
              .forEach(e -> assertTrue(complianceViolation.contains(e)));
    }

    @Test
    void testTrailingSpacesInHeaderNameNoCustom0() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 204 No Content\r\n" +
                        "Access-Control-Allow-Headers : Origin\r\n" +
                        "Other: value\r\n" +
                        "\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("204", uriOrStatus);
        assertEquals("No Content", versionOrReason);
        assertTrue(bad.contains("Illegal character "));
    }

    @Test
    void testNoColon7230() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Name\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, HttpCompliance.RFC7230_LEGACY);
        parseAll(parser, buffer);
        assertTrue(bad.contains("Illegal character"));
        assertTrue(complianceViolation.isEmpty());
    }

    @Test
    void testHeaderParseDirect() {
        ByteBuffer b0 = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Header1: value1\r\n" +
                        "Header2:   value 2a  \r\n" +
                        "Header3: 3\r\n" +
                        "Header4:value4\r\n" +
                        "Server5: notServer\r\n" +
                        "HostHeader: notHost\r\n" +
                        "Connection: close\r\n" +
                        "Accept-Encoding: gzip, deflated\r\n" +
                        "Accept: unknown\r\n" +
                        "\r\n");
        ByteBuffer buffer = BufferUtils.allocateDirect(b0.capacity());
        int pos = BufferUtils.flipToFill(buffer);
        BufferUtils.put(b0, buffer);
        BufferUtils.flipToFlush(buffer, pos);

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("Host", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals("Header1", hdr[1]);
        assertEquals("value1", val[1]);
        assertEquals("Header2", hdr[2]);
        assertEquals("value 2a", val[2]);
        assertEquals("Header3", hdr[3]);
        assertEquals("3", val[3]);
        assertEquals("Header4", hdr[4]);
        assertEquals("value4", val[4]);
        assertEquals("Server5", hdr[5]);
        assertEquals("notServer", val[5]);
        assertEquals("HostHeader", hdr[6]);
        assertEquals("notHost", val[6]);
        assertEquals("Connection", hdr[7]);
        assertEquals("close", val[7]);
        assertEquals("Accept-Encoding", hdr[8]);
        assertEquals("gzip, deflated", val[8]);
        assertEquals("Accept", hdr[9]);
        assertEquals("unknown", val[9]);
        assertEquals(9, headers);
    }

    @Test
    void testHeaderParseCRLF() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Header1: value1\r\n" +
                        "Header2:   value 2a  \r\n" +
                        "Header3: 3\r\n" +
                        "Header4:value4\r\n" +
                        "Server5: notServer\r\n" +
                        "HostHeader: notHost\r\n" +
                        "Connection: close\r\n" +
                        "Accept-Encoding: gzip, deflated\r\n" +
                        "Accept: unknown\r\n" +
                        "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("Host", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals("Header1", hdr[1]);
        assertEquals("value1", val[1]);
        assertEquals("Header2", hdr[2]);
        assertEquals("value 2a", val[2]);
        assertEquals("Header3", hdr[3]);
        assertEquals("3", val[3]);
        assertEquals("Header4", hdr[4]);
        assertEquals("value4", val[4]);
        assertEquals("Server5", hdr[5]);
        assertEquals("notServer", val[5]);
        assertEquals("HostHeader", hdr[6]);
        assertEquals("notHost", val[6]);
        assertEquals("Connection", hdr[7]);
        assertEquals("close", val[7]);
        assertEquals("Accept-Encoding", hdr[8]);
        assertEquals("gzip, deflated", val[8]);
        assertEquals("Accept", hdr[9]);
        assertEquals("unknown", val[9]);
        assertEquals(9, headers);
    }

    @Test
    void testHeaderParseLF() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\n" +
                        "Host: localhost\n" +
                        "Header1: value1\n" +
                        "Header2:   value 2a value 2b  \n" +
                        "Header3: 3\n" +
                        "Header4:value4\n" +
                        "Server5: notServer\n" +
                        "HostHeader: notHost\n" +
                        "Connection: close\n" +
                        "Accept-Encoding: gzip, deflated\n" +
                        "Accept: unknown\n" +
                        "\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("Host", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals("Header1", hdr[1]);
        assertEquals("value1", val[1]);
        assertEquals("Header2", hdr[2]);
        assertEquals("value 2a value 2b", val[2]);
        assertEquals("Header3", hdr[3]);
        assertEquals("3", val[3]);
        assertEquals("Header4", hdr[4]);
        assertEquals("value4", val[4]);
        assertEquals("Server5", hdr[5]);
        assertEquals("notServer", val[5]);
        assertEquals("HostHeader", hdr[6]);
        assertEquals("notHost", val[6]);
        assertEquals("Connection", hdr[7]);
        assertEquals("close", val[7]);
        assertEquals("Accept-Encoding", hdr[8]);
        assertEquals("gzip, deflated", val[8]);
        assertEquals("Accept", hdr[9]);
        assertEquals("unknown", val[9]);
        assertEquals(9, headers);
    }

    @Test
    void testQuoted() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\n" +
                        "Name0: \"value0\"\t\n" +
                        "Name1: \"value\t1\"\n" +
                        "Name2: \"value\t2A\",\"value,2B\"\t\n" +
                        "\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("Name0", hdr[0]);
        assertEquals("\"value0\"", val[0]);
        assertEquals("Name1", hdr[1]);
        assertEquals("\"value\t1\"", val[1]);
        assertEquals("Name2", hdr[2]);
        assertEquals("\"value\t2A\",\"value,2B\"", val[2]);
        assertEquals(2, headers);
    }

    @Test
    void testEncodedHeader() {
        ByteBuffer buffer = BufferUtils.allocate(4096);
        BufferUtils.flipToFill(buffer);
        BufferUtils.put(BufferUtils.toBuffer("GET "), buffer);
        buffer.put("/foo/\u0690/".getBytes(StandardCharsets.UTF_8));
        BufferUtils.put(BufferUtils.toBuffer(" HTTP/1.0\r\n"), buffer);
        BufferUtils.put(BufferUtils.toBuffer("Header1: "), buffer);
        buffer.put("\u00e6 \u00e6".getBytes(StandardCharsets.ISO_8859_1));
        BufferUtils.put(BufferUtils.toBuffer("  \r\nHeader2: "), buffer);
        buffer.put((byte) -1);
        BufferUtils.put(BufferUtils.toBuffer("\r\n\r\n"), buffer);
        BufferUtils.flipToFlush(buffer, 0);

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/foo/\u0690/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("Header1", hdr[0]);
        assertEquals("\u00e6 \u00e6", val[0]);
        assertEquals("Header2", hdr[1]);
        assertEquals("" + (char) 255, val[1]);
        assertEquals(1, headers);
        assertEquals(null, bad);
    }

    @Test
    void testResponseBufferUpgradeFrom() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 101 Upgrade\r\n" +
                        "Connection: upgrade\r\n" +
                        "Content-Length: 0\r\n" +
                        "Sec-WebSocket-Accept: 4GnyoUP4Sc1JD+2pCbNYAhFYVVA\r\n" +
                        "\r\n" +
                        "FOOGRADE");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        while (!parser.isState(State.END)) {
            parser.parseNext(buffer);
        }

        assertEquals("FOOGRADE", BufferUtils.toUTF8String(buffer));
    }

    @Test
    void testBadMethodEncoding() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "G\u00e6T / HTTP/1.0\r\nHeader0: value0\r\n\n\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertNotNull(bad);
    }

    @Test
    void testBadVersionEncoding() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / H\u00e6P/1.0\r\nHeader0: value0\r\n\n\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertNotNull(bad);
    }

    @Test
    void testBadHeaderEncoding() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n"
                        + "H\u00e6der0: value0\r\n"
                        + "\n\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertNotNull(bad);
    }

    @Test
        // TODO: Parameterize Test
    void testBadHeaderNames() {
        String[] bad = new String[]
                {
                        "Foo\\Bar: value\r\n",
                        "Foo@Bar: value\r\n",
                        "Foo,Bar: value\r\n",
                        "Foo}Bar: value\r\n",
                        "Foo{Bar: value\r\n",
                        "Foo=Bar: value\r\n",
                        "Foo>Bar: value\r\n",
                        "Foo<Bar: value\r\n",
                        "Foo)Bar: value\r\n",
                        "Foo(Bar: value\r\n",
                        "Foo?Bar: value\r\n",
                        "Foo\"Bar: value\r\n",
                        "Foo/Bar: value\r\n",
                        "Foo]Bar: value\r\n",
                        "Foo[Bar: value\r\n",
                };

        for (int i = 0; i < bad.length; i++) {
            ByteBuffer buffer = BufferUtils.toBuffer(
                    "GET / HTTP/1.0\r\n" + bad[i] + "\r\n");

            HttpParser.RequestHandler handler = new Handler();
            HttpParser parser = new HttpParser(handler);
            parseAll(parser, buffer);
            assertNotNull(this.bad);
        }
    }

    @Test
    void testHeaderTab() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Header: value\talternate\r\n" +
                        "\n\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.1", versionOrReason);
        assertEquals("Host", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals("Header", hdr[1]);
        assertEquals("value\talternate", val[1]);
    }

    @Test
    void testCaseSensitiveMethod() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "gEt / http/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, -1, HttpCompliance.RFC7230_LEGACY);
        parseAll(parser, buffer);
        assertNull(bad);
        assertEquals("GET", methodOrVersion);
        assertTrue(complianceViolation.contains(HttpComplianceSection.METHOD_CASE_SENSITIVE));
    }

    @Test
    void testCaseSensitiveMethodLegacy() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "gEt / http/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Connection: close\r\n" +
                        "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, -1, HttpCompliance.LEGACY);
        parseAll(parser, buffer);
        assertNull(bad);
        assertEquals("gEt", methodOrVersion);
        assertTrue(complianceViolation.isEmpty());
    }

    @Test
    void testCaseInsensitiveHeader() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / http/1.0\r\n" +
                        "HOST: localhost\r\n" +
                        "cOnNeCtIoN: ClOsE\r\n" +
                        "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, -1, HttpCompliance.RFC7230_LEGACY);
        parseAll(parser, buffer);
        assertNull(bad);
        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("Host", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals("Connection", hdr[1]);
        assertEquals("close", val[1]);
        assertEquals(1, headers);
        assertTrue(complianceViolation.isEmpty());
    }

    @Test
    void testCaseInSensitiveHeaderLegacy() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / http/1.0\r\n" +
                        "HOST: localhost\r\n" +
                        "cOnNeCtIoN: ClOsE\r\n" +
                        "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, -1, HttpCompliance.LEGACY);
        parseAll(parser, buffer);
        assertNull(bad);
        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("HOST", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals("cOnNeCtIoN", hdr[1]);
        assertEquals("ClOsE", val[1]);
        assertEquals(1, headers);
        Arrays.asList(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE, HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE, HttpComplianceSection.CASE_INSENSITIVE_FIELD_VALUE_CACHE)
              .forEach(e -> assertTrue(complianceViolation.contains(e)));

    }

    @Test
    void testSplitHeaderParse() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "XXXXSPLIT / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "Header1: value1\r\n" +
                        "Header2:   value 2a  \r\n" +
                        "Header3: 3\r\n" +
                        "Header4:value4\r\n" +
                        "Server5: notServer\r\n" +
                        "\r\nZZZZ");
        buffer.position(2);
        buffer.limit(buffer.capacity() - 2);
        buffer = buffer.slice();

        for (int i = 0; i < buffer.capacity() - 4; i++) {
            HttpParser.RequestHandler handler = new Handler();
            HttpParser parser = new HttpParser(handler);

            buffer.position(2);
            buffer.limit(2 + i);

            if (!parser.parseNext(buffer)) {
                // consumed all
                assertEquals(0, buffer.remaining());

                // parse the rest
                buffer.limit(buffer.capacity() - 2);
                parser.parseNext(buffer);
            }

            assertEquals("SPLIT", methodOrVersion);
            assertEquals("/", uriOrStatus);
            assertEquals("HTTP/1.0", versionOrReason);
            assertEquals("Host", hdr[0]);
            assertEquals("localhost", val[0]);
            assertEquals("Header1", hdr[1]);
            assertEquals("value1", val[1]);
            assertEquals("Header2", hdr[2]);
            assertEquals("value 2a", val[2]);
            assertEquals("Header3", hdr[3]);
            assertEquals("3", val[3]);
            assertEquals("Header4", hdr[4]);
            assertEquals("value4", val[4]);
            assertEquals("Server5", hdr[5]);
            assertEquals("notServer", val[5]);
            assertEquals(5, headers);
        }
    }

    @Test
    void testChunkParse() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET /chunk HTTP/1.0\r\n"
                        + "Header1: value1\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "\r\n"
                        + "a;\r\n"
                        + "0123456789\r\n"
                        + "1a\r\n"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n"
                        + "0\r\n"
                        + "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/chunk", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(1, headers);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testBadChunkParse() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET /chunk HTTP/1.0\r\n"
                        + "Header1: value1\r\n"
                        + "Transfer-Encoding: chunked, identity\r\n"
                        + "\r\n"
                        + "a;\r\n"
                        + "0123456789\r\n"
                        + "1a\r\n"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n"
                        + "0\r\n"
                        + "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/chunk", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertTrue(bad.contains("Bad chunking"));
    }

    @Test
    void testChunkParseTrailer() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET /chunk HTTP/1.0\r\n"
                        + "Header1: value1\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "\r\n"
                        + "a;\r\n"
                        + "0123456789\r\n"
                        + "1a\r\n"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n"
                        + "0\r\n"
                        + "Trailer: value\r\n"
                        + "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/chunk", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(1, headers);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);
        assertEquals(1, trailers.size());
        HttpField trailer1 = trailers.get(0);
        assertEquals("Trailer", trailer1.getName());
        assertEquals("value", trailer1.getValue());

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testChunkParseTrailers() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET /chunk HTTP/1.0\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "\r\n"
                        + "a;\r\n"
                        + "0123456789\r\n"
                        + "1a\r\n"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n"
                        + "0\r\n"
                        + "Trailer: value\r\n"
                        + "Foo: bar\r\n"
                        + "\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/chunk", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(0, headers);
        assertEquals("Transfer-Encoding", hdr[0]);
        assertEquals("chunked", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);
        assertEquals(2, trailers.size());
        HttpField trailer1 = trailers.get(0);
        assertEquals("Trailer", trailer1.getName());
        assertEquals("value", trailer1.getValue());
        HttpField trailer2 = trailers.get(1);
        assertEquals("Foo", trailer2.getName());
        assertEquals("bar", trailer2.getValue());

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testChunkParseBadTrailer() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET /chunk HTTP/1.0\r\n"
                        + "Header1: value1\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "\r\n"
                        + "a;\r\n"
                        + "0123456789\r\n"
                        + "1a\r\n"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n"
                        + "0\r\n"
                        + "Trailer: value");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);

        assertEquals("GET", methodOrVersion);
        assertEquals("/chunk", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(1, headers);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);

        assertTrue(headerCompleted);
        assertTrue(early);
    }

    @Test
    void testChunkParseNoTrailer() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET /chunk HTTP/1.0\r\n"
                        + "Header1: value1\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "\r\n"
                        + "a;\r\n"
                        + "0123456789\r\n"
                        + "1a\r\n"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n"
                        + "0\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);

        assertEquals("GET", methodOrVersion);
        assertEquals("/chunk", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(1, headers);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testStartEOF() {
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);

        assertTrue(early);
        assertEquals(null, bad);
    }

    @Test
    void testEarlyEOF() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET /uri HTTP/1.0\r\n"
                        + "Content-Length: 20\r\n"
                        + "\r\n"
                        + "0123456789");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.atEOF();
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/uri", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals("0123456789", content);

        assertTrue(early);
    }

    @Test
    void testChunkEarlyEOF() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET /chunk HTTP/1.0\r\n"
                        + "Header1: value1\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "\r\n"
                        + "a;\r\n"
                        + "0123456789\r\n");
        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.atEOF();
        parseAll(parser, buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/chunk", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(1, headers);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789", content);

        assertTrue(early);
    }

    @Test
    void testMultiParse() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET /mp HTTP/1.0\r\n"
                        + "Connection: Keep-Alive\r\n"
                        + "Header1: value1\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "\r\n"
                        + "a;\r\n"
                        + "0123456789\r\n"
                        + "1a\r\n"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n"
                        + "0\r\n"

                        + "\r\n"

                        + "POST /foo HTTP/1.0\r\n"
                        + "Connection: Keep-Alive\r\n"
                        + "Header2: value2\r\n"
                        + "Content-Length: 0\r\n"
                        + "\r\n"

                        + "PUT /doodle HTTP/1.0\r\n"
                        + "Connection: close\r\n"
                        + "Header3: value3\r\n"
                        + "Content-Length: 10\r\n"
                        + "\r\n"
                        + "0123456789\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("GET", methodOrVersion);
        assertEquals("/mp", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(2, headers);
        assertEquals("Header1", hdr[1]);
        assertEquals("value1", val[1]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);

        parser.reset();
        init();
        parser.parseNext(buffer);
        assertEquals("POST", methodOrVersion);
        assertEquals("/foo", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(2, headers);
        assertEquals("Header2", hdr[1]);
        assertEquals("value2", val[1]);
        assertEquals(null, content);

        parser.reset();
        init();
        parser.parseNext(buffer);
        parser.atEOF();
        assertEquals("PUT", methodOrVersion);
        assertEquals("/doodle", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(2, headers);
        assertEquals("Header3", hdr[1]);
        assertEquals("value3", val[1]);
        assertEquals("0123456789", content);
    }

    @Test
    void testMultiParseEarlyEOF() {
        ByteBuffer buffer0 = BufferUtils.toBuffer(
                "GET /mp HTTP/1.0\r\n"
                        + "Connection: Keep-Alive\r\n");

        ByteBuffer buffer1 = BufferUtils.toBuffer("Header1: value1\r\n"
                + "Transfer-Encoding: chunked\r\n"
                + "\r\n"
                + "a;\r\n"
                + "0123456789\r\n"
                + "1a\r\n"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n"
                + "0\r\n"

                + "\r\n"

                + "POST /foo HTTP/1.0\r\n"
                + "Connection: Keep-Alive\r\n"
                + "Header2: value2\r\n"
                + "Content-Length: 0\r\n"
                + "\r\n"

                + "PUT /doodle HTTP/1.0\r\n"
                + "Connection: close\r\n"
                + "Header3: value3\r\n"
                + "Content-Length: 10\r\n"
                + "\r\n"
                + "0123456789\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer0);
        parser.atEOF();
        parser.parseNext(buffer1);
        assertEquals("GET", methodOrVersion);
        assertEquals("/mp", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(2, headers);
        assertEquals("Header1", hdr[1]);
        assertEquals("value1", val[1]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);

        parser.reset();
        init();
        parser.parseNext(buffer1);
        assertEquals("POST", methodOrVersion);
        assertEquals("/foo", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(2, headers);
        assertEquals("Header2", hdr[1]);
        assertEquals("value2", val[1]);
        assertEquals(null, content);

        parser.reset();
        init();
        parser.parseNext(buffer1);
        assertEquals("PUT", methodOrVersion);
        assertEquals("/doodle", uriOrStatus);
        assertEquals("HTTP/1.0", versionOrReason);
        assertEquals(2, headers);
        assertEquals("Header3", hdr[1]);
        assertEquals("value3", val[1]);
        assertEquals("0123456789", content);
    }

    @Test
    void testResponseParse0() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 200 Correct\r\n"
                        + "Content-Length: 10\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "\r\n"
                        + "0123456789\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("200", uriOrStatus);
        assertEquals("Correct", versionOrReason);
        assertEquals(10, content.length());
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testResponseParse1() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 304 Not-Modified\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("304", uriOrStatus);
        assertEquals("Not-Modified", versionOrReason);
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testResponseParse2() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 204 No-Content\r\n"
                        + "Header: value\r\n"
                        + "\r\n"

                        + "HTTP/1.1 200 Correct\r\n"
                        + "Content-Length: 10\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "\r\n"
                        + "0123456789\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("204", uriOrStatus);
        assertEquals("No-Content", versionOrReason);
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);

        parser.reset();
        init();

        parser.parseNext(buffer);
        parser.atEOF();
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("200", uriOrStatus);
        assertEquals("Correct", versionOrReason);
        assertEquals(content.length(), 10);
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testResponseParse3() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 200\r\n"
                        + "Content-Length: 10\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "\r\n"
                        + "0123456789\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("200", uriOrStatus);
        assertNull(versionOrReason);
        assertEquals(content.length(), 10);
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testResponseParse4() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 200 \r\n"
                        + "Content-Length: 10\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "\r\n"
                        + "0123456789\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("200", uriOrStatus);
        assertEquals(null, versionOrReason);
        assertEquals(content.length(), 10);
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testResponseEOFContent() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 200 \r\n"
                        + "Content-Type: text/plain\r\n"
                        + "\r\n"
                        + "0123456789\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.atEOF();
        parser.parseNext(buffer);

        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("200", uriOrStatus);
        assertEquals(null, versionOrReason);
        assertEquals(12, content.length());
        assertEquals("0123456789\r\n", content);
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testResponse304WithContentLength() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 304 found\r\n"
                        + "Content-Length: 10\r\n"
                        + "\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("304", uriOrStatus);
        assertEquals("found", versionOrReason);
        assertNull(content);
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testResponse101WithTransferEncoding() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 101 switching protocols\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("101", uriOrStatus);
        assertEquals("switching protocols", versionOrReason);
        assertNull(content);
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
    }

    @Test
    void testResponseReasonIso8859_1() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 302 déplacé temporairement\r\n"
                        + "Content-Length: 0\r\n"
                        + "\r\n", StandardCharsets.ISO_8859_1);

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("302", uriOrStatus);
        assertEquals("déplacé temporairement", versionOrReason);
    }

    @Test
    void testSeekEOF() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 200 OK\r\n"
                        + "Content-Length: 0\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                        + "\r\n" // extra CRLF ignored
                        + "HTTP/1.1 400 OK\r\n");  // extra data causes close ??

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals("HTTP/1.1", methodOrVersion);
        assertEquals("200", uriOrStatus);
        assertEquals("OK", versionOrReason);
        assertNull(content);
        assertTrue(headerCompleted);
        assertTrue(messageCompleted);

        parser.close();
        parser.reset();
        parser.parseNext(buffer);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testNoURI() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET\r\n"
                        + "Content-Length: 0\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals(null, methodOrVersion);
        assertEquals("No URI", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testNoURI2() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET \r\n"
                        + "Content-Length: 0\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals(null, methodOrVersion);
        assertEquals("No URI", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testUnknownResponseVersion() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HPPT/7.7 200 OK\r\n"
                        + "Content-Length: 0\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertNull(methodOrVersion);
        assertEquals("Unknown Version", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());

    }

    @Test
    void testNoStatus() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1\r\n"
                        + "Content-Length: 0\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertNull(methodOrVersion);
        assertEquals("No Status", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testNoStatus2() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "HTTP/1.1 \r\n"
                        + "Content-Length: 0\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.ResponseHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertNull(methodOrVersion);
        assertEquals("No Status", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testBadRequestVersion() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HPPT/7.7\r\n"
                        + "Content-Length: 0\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertNull(methodOrVersion);
        assertEquals("Unknown Version", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());

        buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.01\r\n"
                        + "Content-Length: 0\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        handler = new Handler();
        parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertNull(methodOrVersion);
        assertEquals("Unknown Version", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testBadCR() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n"
                        + "Content-Length: 0\r"
                        + "Connection: close\r"
                        + "\r");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals("Bad EOL", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());

        buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r"
                        + "Content-Length: 0\r"
                        + "Connection: close\r"
                        + "\r");

        handler = new Handler();
        parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals("Bad EOL", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testBadContentLength0() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n"
                        + "Content-Length: abc\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals("GET", methodOrVersion);
        assertEquals("Invalid Content-Length Value", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testBadContentLength1() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n"
                        + "Content-Length: 9999999999999999999999999999999999999999999999\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals("GET", methodOrVersion);
        assertEquals("Invalid Content-Length Value", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testBadContentLength2() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.0\r\n"
                        + "Content-Length: 1.5\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals("GET", methodOrVersion);
        assertEquals("Invalid Content-Length Value", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testMultipleContentLengthWithLargerThenCorrectValue() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "POST / HTTP/1.1\r\n"
                        + "Content-Length: 2\r\n"
                        + "Content-Length: 1\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                        + "X");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals("POST", methodOrVersion);
        assertEquals("Multiple Content-Lengths", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testMultipleContentLengthWithCorrectThenLargerValue() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "POST / HTTP/1.1\r\n"
                        + "Content-Length: 1\r\n"
                        + "Content-Length: 2\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                        + "X");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);

        parser.parseNext(buffer);
        assertEquals("POST", methodOrVersion);
        assertEquals("Multiple Content-Lengths", bad);
        assertFalse(buffer.hasRemaining());
        assertEquals(HttpParser.State.CLOSE, parser.getState());
        parser.atEOF();
        parser.parseNext(BufferUtils.EMPTY_BUFFER);
        assertEquals(HttpParser.State.CLOSED, parser.getState());
    }

    @Test
    void testTransferEncodingChunkedThenContentLength() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "POST /chunk HTTP/1.1\r\n"
                        + "Host: localhost\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "Content-Length: 1\r\n"
                        + "\r\n"
                        + "1\r\n"
                        + "X\r\n"
                        + "0\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, HttpCompliance.RFC2616_LEGACY);
        parseAll(parser, buffer);

        assertEquals("POST", methodOrVersion);
        assertEquals("/chunk", uriOrStatus);
        assertEquals("HTTP/1.1", versionOrReason);
        assertEquals("X", content);

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);

        assertTrue(complianceViolation.contains(HttpComplianceSection.TRANSFER_ENCODING_WITH_CONTENT_LENGTH));
    }

    @Test
    void testContentLengthThenTransferEncodingChunked() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "POST /chunk HTTP/1.1\r\n"
                        + "Host: localhost\r\n"
                        + "Content-Length: 1\r\n"
                        + "Transfer-Encoding: chunked\r\n"
                        + "\r\n"
                        + "1\r\n"
                        + "X\r\n"
                        + "0\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler, HttpCompliance.RFC2616_LEGACY);
        parseAll(parser, buffer);

        assertEquals("POST", methodOrVersion);
        assertEquals("/chunk", uriOrStatus);
        assertEquals("HTTP/1.1", versionOrReason);
        assertEquals("X", content);

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);

        assertTrue(complianceViolation.contains(HttpComplianceSection.TRANSFER_ENCODING_WITH_CONTENT_LENGTH));
    }

    @Test
    void testHost() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Host: host\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("host", host);
        assertEquals(0, port);
    }

    @Test
    void testUriHost11() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET http://host/ HTTP/1.1\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("No Host", bad);
        assertEquals("http://host/", uriOrStatus);
        assertEquals(0, port);
    }

    @Test
    void testUriHost10() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET http://host/ HTTP/1.0\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertNull(bad);
        assertEquals("http://host/", uriOrStatus);
        assertEquals(0, port);
    }

    @Test
    void testNoHost() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("No Host", bad);
    }

    @Test
    void testIPHost() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Host: 192.168.0.1\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("192.168.0.1", host);
        assertEquals(0, port);
    }

    @Test
    void testIPv6Host() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Host: [::1]\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("[::1]", host);
        assertEquals(0, port);
    }

    @Test
    void testBadIPv6Host() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Host: [::1\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertTrue(bad.contains("Bad"));
    }

    @Test
    void testHostPort() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Host: myhost:8888\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("myhost", host);
        assertEquals(8888, port);
    }

    @Test
    void testHostBadPort() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Host: myhost:testBadPort\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertTrue(bad.contains("Bad Host"));
    }

    @Test
    void testIPHostPort() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Host: 192.168.0.1:8888\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("192.168.0.1", host);
        assertEquals(8888, port);
    }

    @Test
    void testIPv6HostPort() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Host: [::1]:8888\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals("[::1]", host);
        assertEquals(8888, port);
    }

    @Test
    void testEmptyHostPort() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n"
                        + "Host:\r\n"
                        + "Connection: close\r\n"
                        + "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);
        assertEquals(null, host);
        assertEquals(null, bad);
    }

    @Test
    @SuppressWarnings("ReferenceEquality")
    void testCachedField() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n" +
                        "Host: www.smh.com.au\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);
        assertEquals("www.smh.com.au", parser.getFieldCache().get("Host: www.smh.com.au").getValue());
        HttpField field = fields.get(0);

        buffer.position(0);
        parseAll(parser, buffer);
        assertTrue(field == fields.get(0));
    }

    @Test
    void testParseRequest() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "GET / HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Header1: value1\r\n" +
                        "Connection: close\r\n" +
                        "Accept-Encoding: gzip, deflated\r\n" +
                        "Accept: unknown\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parser.parseNext(buffer);

        assertEquals("GET", methodOrVersion);
        assertEquals("/", uriOrStatus);
        assertEquals("HTTP/1.1", versionOrReason);
        assertEquals("Host", hdr[0]);
        assertEquals("localhost", val[0]);
        assertEquals("Connection", hdr[2]);
        assertEquals("close", val[2]);
        assertEquals("Accept-Encoding", hdr[3]);
        assertEquals("gzip, deflated", val[3]);
        assertEquals("Accept", hdr[4]);
        assertEquals("unknown", val[4]);
    }

    @Test
    void testHTTP2Preface() {
        ByteBuffer buffer = BufferUtils.toBuffer(
                "PRI * HTTP/2.0\r\n" +
                        "\r\n" +
                        "SM\r\n" +
                        "\r\n");

        HttpParser.RequestHandler handler = new Handler();
        HttpParser parser = new HttpParser(handler);
        parseAll(parser, buffer);

        assertTrue(headerCompleted);
        assertTrue(messageCompleted);
        assertEquals("PRI", methodOrVersion);
        assertEquals("*", uriOrStatus);
        assertEquals("HTTP/2.0", versionOrReason);
        assertEquals(-1, headers);
        assertEquals(null, bad);
    }

    @BeforeEach
    void init() {
        bad = null;
        content = null;
        methodOrVersion = null;
        uriOrStatus = null;
        versionOrReason = null;
        hdr = null;
        val = null;
        headers = 0;
        headerCompleted = false;
        messageCompleted = false;
        complianceViolation.clear();
    }

    private class Handler implements HttpParser.RequestHandler, HttpParser.ResponseHandler, HttpParser.ComplianceHandler {
        @Override
        public boolean content(ByteBuffer ref) {
            if (content == null)
                content = "";
            String c = BufferUtils.toString(ref, StandardCharsets.UTF_8);
            content = content + c;
            ref.position(ref.limit());
            return false;
        }

        @Override
        public boolean startRequest(String method, String uri, HttpVersion version) {
            fields.clear();
            trailers.clear();
            headers = -1;
            hdr = new String[10];
            val = new String[10];
            methodOrVersion = method;
            uriOrStatus = uri;
            versionOrReason = version == null ? null : version.getValue();
            messageCompleted = false;
            headerCompleted = false;
            early = false;
            return false;
        }

        @Override
        public void parsedHeader(HttpField field) {
            fields.add(field);
            hdr[++headers] = field.getName();
            val[headers] = field.getValue();

            if (field instanceof HostPortHttpField) {
                HostPortHttpField hpfield = (HostPortHttpField) field;
                host = hpfield.getHost();
                port = hpfield.getPort();
            }
        }

        @Override
        public boolean headerComplete() {
            content = null;
            headerCompleted = true;
            return false;
        }

        @Override
        public void parsedTrailer(HttpField field) {
            trailers.add(field);
        }

        @Override
        public boolean contentComplete() {
            return false;
        }

        @Override
        public boolean messageComplete() {
            messageCompleted = true;
            return true;
        }

        @Override
        public void badMessage(BadMessageException failure) {
            String reason = failure.getReason();
            bad = reason == null ? String.valueOf(failure.getCode()) : reason;
        }

        @Override
        public boolean startResponse(HttpVersion version, int status, String reason) {
            fields.clear();
            trailers.clear();
            methodOrVersion = version.getValue();
            uriOrStatus = Integer.toString(status);
            versionOrReason = reason;
            headers = -1;
            hdr = new String[10];
            val = new String[10];
            messageCompleted = false;
            headerCompleted = false;
            return false;
        }

        @Override
        public void earlyEOF() {
            early = true;
        }

        @Override
        public int getHeaderCacheSize() {
            return 4096;
        }

        @Override
        public void onComplianceViolation(HttpCompliance compliance, HttpComplianceSection violation, String reason) {
            complianceViolation.add(violation);
        }
    }
}
