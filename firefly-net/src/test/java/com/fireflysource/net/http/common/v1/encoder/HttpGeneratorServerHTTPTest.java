package com.fireflysource.net.http.common.v1.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.exception.BadMessageException;
import com.fireflysource.net.http.common.model.HttpField;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpVersion;
import com.fireflysource.net.http.common.model.MetaData;
import com.fireflysource.net.http.common.v1.decoder.HttpParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class HttpGeneratorServerHTTPTest {
    public final static String CONTENT = "The quick brown fox jumped over the lazy dog.\nNow is the time for all good men to come to the aid of the party\nThe moon is blue to a fish in love.\n";
    private String content;
    private String reason;

    public static Stream<Arguments> data() {
        Result[] results = {
                new Result(200, null, -1, null, false),
                new Result(200, null, -1, CONTENT, false),
                new Result(200, null, CONTENT.length(), null, true),
                new Result(200, null, CONTENT.length(), CONTENT, false),
                new Result(200, "text/html", -1, null, true),
                new Result(200, "text/html", -1, CONTENT, false),
                new Result(200, "text/html", CONTENT.length(), null, true),
                new Result(200, "text/html", CONTENT.length(), CONTENT, false)
        };

        ArrayList<Arguments> data = new ArrayList<>();

        // For each test result
        for (Result result : results) {
            // Loop over HTTP versions
            for (int v = 10; v <= 11; v++) {
                // Loop over chunks
                for (int chunks = 1; chunks <= 6; chunks++) {
                    // Loop over Connection values
                    for (ConnectionType connection : ConnectionType.values()) {
                        if (connection.isSupportedByHttp(v)) {
                            data.add(Arguments.of(new Run(result, v, chunks, connection)));
                        }
                    }
                }
            }
        }

        return data.stream();
    }

    @ParameterizedTest
    @MethodSource("data")
    void testHTTP(Run run) throws Exception {
        Handler handler = new Handler();

        HttpGenerator gen = new HttpGenerator();

        String msg = run.toString();

        run.result.getHttpFields().clear();

        String response = run.result.build(run.httpVersion, gen, "OK\r\nTest", run.connection.val, null, run.chunks);

        HttpParser parser = new HttpParser(handler);
        parser.setHeadResponse(run.result.head);

        parser.parseNext(BufferUtils.toBuffer(response));

        if (run.result.body != null)
            assertEquals(run.result.body, this.content, msg);

        // TODO: Break down rationale more clearly, these should be separate checks and/or assertions
        if (run.httpVersion == 10)
            assertTrue(gen.isPersistent() || run.result.contentLength >= 0 || EnumSet.of(ConnectionType.CLOSE, ConnectionType.KEEP_ALIVE, ConnectionType.NONE).contains(run.connection), msg);
        else
            assertTrue(gen.isPersistent() || EnumSet.of(ConnectionType.CLOSE, ConnectionType.TE_CLOSE).contains(run.connection), msg);

        assertEquals("OK??Test", reason);

        if (content == null)
            assertNull(run.result.body, msg);
        else
            assertTrue(run.result.contentLength == content.length() || run.result.contentLength == -1, msg);
    }

    private enum ConnectionType {
        NONE(null, 9, 10, 11),
        KEEP_ALIVE("keep-alive", 9, 10, 11),
        CLOSE("close", 9, 10, 11),
        TE_CLOSE("TE, close", 11);

        private String val;
        private int[] supportedHttpVersions;

        ConnectionType(String val, int... supportedHttpVersions) {
            this.val = val;
            this.supportedHttpVersions = supportedHttpVersions;
        }

        public boolean isSupportedByHttp(int version) {
            for (int supported : supportedHttpVersions) {
                if (supported == version) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class Result {
        private final String body;
        private final int code;
        private final boolean head;
        private HttpFields fields = new HttpFields();
        private String connection;
        private int contentLength;
        private String contentType;
        private String other;
        private String te;

        private Result(int code, String contentType, int contentLength, String content, boolean head) {
            this.code = code;
            this.contentType = contentType;
            this.contentLength = contentLength;
            other = "value";
            body = content;
            this.head = head;
        }

        private String build(int version, HttpGenerator gen, String reason, String connection, String te, int nchunks) throws Exception {
            String response = "";
            this.connection = connection;
            this.te = te;

            if (contentType != null)
                fields.put("Content-Type", contentType);
            if (contentLength >= 0)
                fields.put("Content-Length", "" + contentLength);
            if (this.connection != null)
                fields.put("Connection", this.connection);
            if (this.te != null)
                fields.put("Transfer-Encoding", this.te);
            if (other != null)
                fields.put("Other", other);

            ByteBuffer source = body == null ? null : BufferUtils.toBuffer(body);
            ByteBuffer[] chunks = new ByteBuffer[nchunks];
            ByteBuffer content = null;
            int c = 0;
            if (source != null) {
                for (int i = 0; i < nchunks; i++) {
                    chunks[i] = source.duplicate();
                    chunks[i].position(i * (source.capacity() / nchunks));
                    if (i > 0)
                        chunks[i - 1].limit(chunks[i].position());
                }
                content = chunks[c++];
            }
            ByteBuffer header = null;
            ByteBuffer chunk = null;
            MetaData.Response info = null;

            loop:
            while (true) {
                // if we have unwritten content
                if (source != null && content != null && content.remaining() == 0 && c < nchunks)
                    content = chunks[c++];

                // Generate
                boolean last = !BufferUtils.hasContent(content);

                HttpGenerator.Result result = gen.generateResponse(info, head, header, chunk, content, last);

                switch (result) {
                    case NEED_INFO:
                        info = new MetaData.Response(HttpVersion.fromVersion(version), code, reason, fields, contentLength);
                        continue;

                    case NEED_HEADER:
                        header = BufferUtils.allocate(2048);
                        continue;

                    case NEED_CHUNK:
                        chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
                        continue;

                    case NEED_CHUNK_TRAILER:
                        chunk = BufferUtils.allocate(2048);
                        continue;


                    case FLUSH:
                        if (BufferUtils.hasContent(header)) {
                            response += BufferUtils.toString(header);
                            header.position(header.limit());
                        }
                        if (BufferUtils.hasContent(chunk)) {
                            response += BufferUtils.toString(chunk);
                            chunk.position(chunk.limit());
                        }
                        if (BufferUtils.hasContent(content)) {
                            response += BufferUtils.toString(content);
                            content.position(content.limit());
                        }
                        break;

                    case CONTINUE:
                        continue;

                    case SHUTDOWN_OUT:
                        break;

                    case DONE:
                        break loop;
                }
            }
            return response;
        }

        @Override
        public String toString() {
            return "[" + code + "," + contentType + "," + contentLength + "," + (body == null ? "null" : "content") + "]";
        }

        public HttpFields getHttpFields() {
            return fields;
        }
    }

    private static class Run {
        private Result result;
        private ConnectionType connection;
        private int httpVersion;
        private int chunks;

        public Run(Result result, int ver, int chunks, ConnectionType connection) {
            this.result = result;
            this.httpVersion = ver;
            this.chunks = chunks;
            this.connection = connection;
        }

        @Override
        public String toString() {
            return String.format("result=%s,version=%d,chunks=%d,connection=%s", result, httpVersion, chunks, connection.name());
        }
    }

    private class Handler implements HttpParser.ResponseHandler {
        @Override
        public boolean content(ByteBuffer ref) {
            if (content == null)
                content = "";
            content += BufferUtils.toString(ref);
            ref.position(ref.limit());
            return false;
        }

        @Override
        public void earlyEOF() {
        }

        @Override
        public boolean headerComplete() {
            content = null;
            return false;
        }

        @Override
        public boolean contentComplete() {
            return false;
        }

        @Override
        public boolean messageComplete() {
            return true;
        }

        @Override
        public void parsedHeader(HttpField field) {
        }

        @Override
        public boolean startResponse(HttpVersion version, int status, String reason) {
            HttpGeneratorServerHTTPTest.this.reason = reason;
            return false;
        }

        @Override
        public void badMessage(BadMessageException failure) {
            throw failure;
        }

        @Override
        public int getHeaderCacheSize() {
            return 4096;
        }
    }
}
