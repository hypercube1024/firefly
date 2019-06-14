package com.fireflysource.net.http.common.v1.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.model.*;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpGeneratorClientTest {

    @Test
    void testGETRequestNoContent() {
        ByteBuffer header = BufferUtils.allocate(2048);
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateRequest(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        Info info = new Info("GET", "/index.html");
        info.getFields().add("Host", "something");
        info.getFields().add("User-Agent", "test");
        assertFalse(gen.isChunking());

        result = gen.generateRequest(info, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateRequest(info, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        assertFalse(gen.isChunking());
        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateRequest(null, null, null, null, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());
        assertFalse(gen.isChunking());

        assertEquals(0, gen.getContentPrepared());
        assertTrue(out.contains("GET /index.html HTTP/1.1"));
        assertFalse(out.contains("Content-Length"));
    }

    @Test
    void testEmptyHeaders() {
        ByteBuffer header = BufferUtils.allocate(2048);
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateRequest(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        Info info = new Info("GET", "/index.html");
        info.getFields().add("Host", "something");
        info.getFields().add("Null", null);
        info.getFields().add("Empty", "");
        assertFalse(gen.isChunking());

        result = gen.generateRequest(info, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateRequest(info, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        assertFalse(gen.isChunking());
        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateRequest(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());
        assertFalse(gen.isChunking());

        assertEquals(0, gen.getContentPrepared());
        assertTrue(out.contains("GET /index.html HTTP/1.1"));
        assertFalse(out.contains("Content-Length"));
        assertTrue(out.contains("Empty:"));
        assertFalse(out.contains("Null:"));
    }

    @Test
    void testPOSTRequestNoContent() {
        ByteBuffer header = BufferUtils.allocate(2048);
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateRequest(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        Info info = new Info("POST", "/index.html");
        info.getFields().add("Host", "something");
        info.getFields().add("User-Agent", "test");
        assertFalse(gen.isChunking());

        result = gen.generateRequest(info, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateRequest(info, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        assertFalse(gen.isChunking());
        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateRequest(null, null, null, null, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());
        assertFalse(gen.isChunking());

        assertEquals(0, gen.getContentPrepared());
        assertTrue(out.contains("POST /index.html HTTP/1.1"));
        assertTrue(out.contains("Content-Length: 0"));
    }

    @Test
    void testRequestWithContent() {
        String out;
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer content0 = BufferUtils.toBuffer("Hello World. The quick brown fox jumped over the lazy dog.");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateRequest(null, null, null, content0, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        Info info = new Info("POST", "/index.html");
        info.getFields().add("Host", "something");
        info.getFields().add("User-Agent", "test");

        result = gen.generateRequest(info, null, null, content0, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateRequest(info, header, null, content0, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        assertFalse(gen.isChunking());

        out = BufferUtils.toString(header);
        BufferUtils.clear(header);
        out += BufferUtils.toString(content0);
        BufferUtils.clear(content0);

        result = gen.generateRequest(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());
        assertFalse(gen.isChunking());


        assertTrue(out.contains("POST /index.html HTTP/1.1"));
        assertTrue(out.contains("Host: something"));
        assertTrue(out.contains("Content-Length: 58"));
        assertTrue(out.contains("Hello World. The quick brown fox jumped over the lazy dog."));

        assertEquals(58, gen.getContentPrepared());
    }

    @Test
    void testRequestWithChunkedContent() {
        String out;
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
        ByteBuffer content0 = BufferUtils.toBuffer("Hello World. ");
        ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog.");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateRequest(null, null, null, null, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        Info info = new Info("POST", "/index.html");
        info.getFields().add("Host", "something");
        info.getFields().add("User-Agent", "test");

        result = gen.generateRequest(info, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateRequest(info, header, null, null, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        assertTrue(gen.isChunking());
        out = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateRequest(info, header, chunk, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        assertTrue(gen.isChunking());

        out += BufferUtils.toString(chunk);
        BufferUtils.clear(chunk);
        out += BufferUtils.toString(content0);
        BufferUtils.clear(content0);

        result = gen.generateRequest(null, null, null, content1, false);
        assertEquals(HttpGenerator.Result.NEED_CHUNK, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        result = gen.generateRequest(null, null, chunk, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        assertTrue(gen.isChunking());

        out += BufferUtils.toString(chunk);
        BufferUtils.clear(chunk);
        out += BufferUtils.toString(content1);
        BufferUtils.clear(content1);

        result = gen.generateRequest(null, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        assertTrue(gen.isChunking());

        result = gen.generateRequest(null, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        out += BufferUtils.toString(chunk);
        BufferUtils.clear(chunk);
        assertFalse(gen.isChunking());

        result = gen.generateRequest(null, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertTrue(out.contains("POST /index.html HTTP/1.1"));
        assertTrue(out.contains("Host: something"));
        assertTrue(out.contains("Transfer-Encoding: chunked"));
        assertTrue(out.contains("\r\nD\r\nHello World. \r\n"));
        assertTrue(out.contains("\r\n2D\r\nThe quick brown fox jumped over the lazy dog.\r\n"));
        assertTrue(out.contains("\r\n0\r\n\r\n"));

        assertEquals(58, gen.getContentPrepared());
    }

    @Test
    void testRequestWithKnownContent() {
        String out;
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
        ByteBuffer content0 = BufferUtils.toBuffer("Hello World. ");
        ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog.");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateRequest(null, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        Info info = new Info("POST", "/index.html", 58);
        info.getFields().add("Host", "something");
        info.getFields().add("User-Agent", "test");

        result = gen.generateRequest(info, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateRequest(info, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        assertFalse(gen.isChunking());
        out = BufferUtils.toString(header);
        BufferUtils.clear(header);
        out += BufferUtils.toString(content0);
        BufferUtils.clear(content0);

        result = gen.generateRequest(null, null, null, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        assertFalse(gen.isChunking());
        out += BufferUtils.toString(content1);
        BufferUtils.clear(content1);

        result = gen.generateRequest(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        assertFalse(gen.isChunking());

        result = gen.generateRequest(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());
        out += BufferUtils.toString(chunk);
        BufferUtils.clear(chunk);

        assertTrue(out.contains("POST /index.html HTTP/1.1"));
        assertTrue(out.contains("Host: something"));
        assertTrue(out.contains("Content-Length: 58"));
        assertTrue(out.contains("\r\n\r\nHello World. The quick brown fox jumped over the lazy dog."));

        assertEquals(58, gen.getContentPrepared());
    }

    @Test
    void testAddFields() {
        ByteBuffer header = BufferUtils.allocate(2048);
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateRequest(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        Info info = new Info("GET", "/index.html");
        info.getFields().add("Host", "something1");
        info.getFields().add("User-Agent", "test1");
        info.getFields().add("Connection", HttpHeaderValue.KEEP_ALIVE.getValue());
        List<String> values = info.getFields().getValuesList("Connection");
        if (values != null && !values.isEmpty()) {
            info.getFields().remove("Connection");
            List<String> newValues = new LinkedList<>(values);
            newValues.add("Upgrade");
            newValues.add("HTTP2-Settings");
            info.getFields().addCSV("Connection", newValues.toArray(new String[0]));
        }
        assertFalse(gen.isChunking());

        result = gen.generateRequest(info, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateRequest(info, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        assertFalse(gen.isChunking());
        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateRequest(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());
        assertFalse(gen.isChunking());

        System.out.println(out);
        assertTrue(out.contains("keep-alive, Upgrade, HTTP2-Settings"));
    }

    class Info extends MetaData.Request {
        Info(String method, String uri) {
            super(method, new HttpURI(uri), HttpVersion.HTTP_1_1, new HttpFields(), -1);
        }

        Info(String method, String uri, int contentLength) {
            super(method, new HttpURI(uri), HttpVersion.HTTP_1_1, new HttpFields(), contentLength);
        }
    }

}
