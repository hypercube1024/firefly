package com.fireflysource.net.http.v1.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.sys.ProjectVersion;
import com.fireflysource.net.http.common.codec.DateGenerator;
import com.fireflysource.net.http.common.exception.BadMessageException;
import com.fireflysource.net.http.common.model.*;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class HttpGeneratorServerTest {
    @Test
    void test_0_9() throws Exception {
        ByteBuffer header = BufferUtils.allocate(8096);
        ByteBuffer content = BufferUtils.toBuffer("0123456789");

        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, content, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_0_9, 200, null, new HttpFields(), 10);
        info.getFields().add("Content-Type", "test/data");
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);

        result = gen.generateResponse(info, false, null, null, content, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        String response = BufferUtils.toString(header);
        BufferUtils.clear(header);
        response += BufferUtils.toString(content);
        BufferUtils.clear(content);

        result = gen.generateResponse(null, false, null, null, content, false);
        assertEquals(HttpGenerator.Result.SHUTDOWN_OUT, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertEquals(10, gen.getContentPrepared());

        assertFalse(response.contains("200 OK"));
        assertFalse(response.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertFalse(response.contains("Content-Length: 10"));
        assertTrue(response.contains("0123456789"));
    }

    @Test
    void testSimple() throws Exception {
        ByteBuffer header = BufferUtils.allocate(8096);
        ByteBuffer content = BufferUtils.toBuffer("0123456789");

        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, content, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), 10);
        info.getFields().add("Content-Type", "test/data");
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);

        result = gen.generateResponse(info, false, null, null, content, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);

        result = gen.generateResponse(info, false, header, null, content, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        String response = BufferUtils.toString(header);
        BufferUtils.clear(header);
        response += BufferUtils.toString(content);
        BufferUtils.clear(content);

        result = gen.generateResponse(null, false, null, null, content, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertEquals(10, gen.getContentPrepared());

        assertTrue(response.contains("HTTP/1.1 200 OK"));
        assertTrue(response.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertTrue(response.contains("Content-Length: 10"));
        assertTrue(response.contains("\r\n0123456789"));
    }

    @Test
    void test204() throws Exception {
        ByteBuffer header = BufferUtils.allocate(8096);
        ByteBuffer content = BufferUtils.toBuffer("0123456789");

        HttpGenerator gen = new HttpGenerator();

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 204, "Foo", new HttpFields(), 10);
        info.getFields().add("Content-Type", "test/data");
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);

        HttpGenerator.Result result = gen.generateResponse(info, false, header, null, content, true);

        assertTrue(gen.isNoContent());
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        String responseheaders = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateResponse(null, false, null, null, content, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertTrue(responseheaders.contains("HTTP/1.1 204 Foo"));
        assertTrue(responseheaders.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertFalse(responseheaders.contains("Content-Length: 10"));

        //Note: the HttpConnection.process() method is responsible for actually
        //excluding the content from the response based on generator.isNoContent()==true
    }


    @Test
    void testComplexChars() throws Exception {
        ByteBuffer header = BufferUtils.allocate(8096);
        ByteBuffer content = BufferUtils.toBuffer("0123456789");

        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, content, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, "ØÆ", new HttpFields(), 10);
        info.getFields().add("Content-Type", "test/data;\r\nextra=value");
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);

        result = gen.generateResponse(info, false, null, null, content, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);

        result = gen.generateResponse(info, false, header, null, content, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        String response = BufferUtils.toString(header);
        BufferUtils.clear(header);
        response += BufferUtils.toString(content);
        BufferUtils.clear(content);

        result = gen.generateResponse(null, false, null, null, content, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertEquals(10, gen.getContentPrepared());

        assertTrue(response.contains("HTTP/1.1 200 ØÆ"));
        assertTrue(response.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertTrue(response.contains("Content-Type: test/data;  extra=value"));
        assertTrue(response.contains("Content-Length: 10"));
        assertTrue(response.contains("\r\n0123456789"));
    }

    @Test
    void testSendServerXPoweredBy() throws Exception {
        ByteBuffer header = BufferUtils.allocate(8096);
        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), -1);
        HttpFields fields = new HttpFields();
        fields.add(HttpHeader.SERVER, "SomeServer");
        fields.add(HttpHeader.X_POWERED_BY, "SomePower");
        MetaData.Response infoF = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, fields, -1);
        String head;

        HttpGenerator gen = new HttpGenerator(true, true);
        gen.generateResponse(info, false, header, null, null, true);
        head = BufferUtils.toString(header);
        BufferUtils.clear(header);
        assertTrue(head.contains("HTTP/1.1 200 OK"));
        assertTrue(head.contains("Server: Firefly(" + ProjectVersion.getValue() + ")"));
        assertTrue(head.contains("X-Powered-By: Firefly(" + ProjectVersion.getValue() + ")"));
        gen.reset();
        gen.generateResponse(infoF, false, header, null, null, true);
        head = BufferUtils.toString(header);
        BufferUtils.clear(header);
        assertTrue(head.contains("HTTP/1.1 200 OK"));
        assertFalse(head.contains("Server: Firefly(" + ProjectVersion.getValue() + ")"));
        assertTrue(head.contains("Server: SomeServer"));
        assertTrue(head.contains("X-Powered-By: Firefly(" + ProjectVersion.getValue() + ")"));
        assertTrue(head.contains("X-Powered-By: SomePower"));
        gen.reset();

        gen = new HttpGenerator(false, false);
        gen.generateResponse(info, false, header, null, null, true);
        head = BufferUtils.toString(header);
        BufferUtils.clear(header);
        assertTrue(head.contains("HTTP/1.1 200 OK"));
        assertFalse(head.contains("Server: Firefly(" + ProjectVersion.getValue() + ")"));
        assertFalse(head.contains("X-Powered-By: Firefly(" + ProjectVersion.getValue() + ")"));
        gen.reset();
        gen.generateResponse(infoF, false, header, null, null, true);
        head = BufferUtils.toString(header);
        BufferUtils.clear(header);
        assertTrue(head.contains("HTTP/1.1 200 OK"));
        assertFalse(head.contains("Server: Firefly(" + ProjectVersion.getValue() + ")"));
        assertTrue(head.contains("Server: SomeServer"));
        assertFalse(head.contains("X-Powered-By: Firefly(" + ProjectVersion.getValue() + ")"));
        assertTrue(head.contains("X-Powered-By: SomePower"));
        gen.reset();
    }

    @Test
    void testResponseIncorrectContentLength() throws Exception {
        ByteBuffer header = BufferUtils.allocate(8096);

        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), 10);
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);
        info.getFields().add("Content-Length", "11");

        result = gen.generateResponse(info, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);

        BadMessageException e = assertThrows(BadMessageException.class, () -> {
            gen.generateResponse(info, false, header, null, null, true);
        });
        assertEquals(500, e.getCode());
    }

    @Test
    void testResponseNoContentPersistent() throws Exception {
        ByteBuffer header = BufferUtils.allocate(8096);

        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), 0);
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);

        result = gen.generateResponse(info, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);

        result = gen.generateResponse(info, false, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        String head = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateResponse(null, false, null, null, null, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertEquals(0, gen.getContentPrepared());
        assertTrue(head.contains("HTTP/1.1 200 OK"));
        assertTrue(head.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertTrue(head.contains("Content-Length: 0"));
    }

    @Test
    void testResponseKnownNoContentNotPersistent() throws Exception {
        ByteBuffer header = BufferUtils.allocate(8096);

        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), 0);
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);
        info.getFields().add("Connection", "close");

        result = gen.generateResponse(info, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);

        result = gen.generateResponse(info, false, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        String head = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateResponse(null, false, null, null, null, false);
        assertEquals(HttpGenerator.Result.SHUTDOWN_OUT, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertEquals(0, gen.getContentPrepared());
        assertTrue(head.contains("HTTP/1.1 200 OK"));
        assertTrue(head.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertTrue(head.contains("Connection: close"));
    }

    @Test
    void testResponseUpgrade() throws Exception {
        ByteBuffer header = BufferUtils.allocate(8096);

        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 101, null, new HttpFields(), -1);
        info.getFields().add("Upgrade", "WebSocket");
        info.getFields().add("Connection", "Upgrade");
        info.getFields().add("Sec-WebSocket-Accept", "123456789==");

        result = gen.generateResponse(info, false, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        String head = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateResponse(info, false, null, null, null, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertEquals(0, gen.getContentPrepared());

        assertTrue(head.startsWith("HTTP/1.1 101 Switching Protocols"));
        assertTrue(head.contains("Upgrade: WebSocket\r\n"));
        assertTrue(head.contains("Connection: Upgrade\r\n"));
    }

    @Test
    void testResponseWithChunkedContent() throws Exception {
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
        ByteBuffer content0 = BufferUtils.toBuffer("Hello World! ");
        ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog. ");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), -1);
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);
        result = gen.generateResponse(info, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, false, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);
        out += BufferUtils.toString(content0);
        BufferUtils.clear(content0);

        result = gen.generateResponse(null, false, null, chunk, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        out += BufferUtils.toString(chunk);
        BufferUtils.clear(chunk);
        out += BufferUtils.toString(content1);
        BufferUtils.clear(content1);

        result = gen.generateResponse(null, false, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, false, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        out += BufferUtils.toString(chunk);
        BufferUtils.clear(chunk);

        result = gen.generateResponse(null, false, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertTrue(out.contains("HTTP/1.1 200 OK"));
        assertTrue(out.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertFalse(out.contains("Content-Length"));
        assertTrue(out.contains("Transfer-Encoding: chunked"));

        assertTrue(out.endsWith(
                "\r\n\r\nD\r\n" +
                        "Hello World! \r\n" +
                        "2E\r\n" +
                        "The quick brown fox jumped over the lazy dog. \r\n" +
                        "0\r\n" +
                        "\r\n"));
    }

    @Test
    void testResponseWithHintedChunkedContent() throws Exception {
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
        ByteBuffer content0 = BufferUtils.toBuffer("Hello World! ");
        ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog. ");
        HttpGenerator gen = new HttpGenerator();
        gen.setPersistent(false);

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), -1);
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);
        info.getFields().add(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED);
        result = gen.generateResponse(info, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, false, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);
        out += BufferUtils.toString(content0);
        BufferUtils.clear(content0);

        result = gen.generateResponse(null, false, null, null, content1, false);
        assertEquals(HttpGenerator.Result.NEED_CHUNK, result);

        result = gen.generateResponse(null, false, null, chunk, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        out += BufferUtils.toString(chunk);
        BufferUtils.clear(chunk);
        out += BufferUtils.toString(content1);
        BufferUtils.clear(content1);

        result = gen.generateResponse(null, false, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, false, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        out += BufferUtils.toString(chunk);
        BufferUtils.clear(chunk);

        result = gen.generateResponse(null, false, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.SHUTDOWN_OUT, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertTrue(out.contains("HTTP/1.1 200 OK"));
        assertTrue(out.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertFalse(out.contains("Content-Length"));
        assertTrue(out.contains("Transfer-Encoding: chunked"));

        assertTrue(out.endsWith(
                "\r\n\r\nD\r\n" +
                        "Hello World! \r\n" +
                        "2E\r\n" +
                        "The quick brown fox jumped over the lazy dog. \r\n" +
                        "0\r\n" +
                        "\r\n"));
    }

    @Test
    void testResponseWithContentAndTrailer() throws Exception {
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
        ByteBuffer trailer = BufferUtils.allocate(4096);
        ByteBuffer content0 = BufferUtils.toBuffer("Hello World! ");
        ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog. ");
        HttpGenerator gen = new HttpGenerator();
        gen.setPersistent(false);

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), -1);
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);
        info.getFields().add(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED);
        info.setTrailerSupplier(() -> {
            HttpFields trailer1 = new HttpFields();
            trailer1.add("T-Name0", "T-ValueA");
            trailer1.add("T-Name0", "T-ValueB");
            trailer1.add("T-Name1", "T-ValueC");
            return trailer1;
        });

        result = gen.generateResponse(info, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, false, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);
        out += BufferUtils.toString(content0);
        BufferUtils.clear(content0);

        result = gen.generateResponse(null, false, null, null, content1, false);
        assertEquals(HttpGenerator.Result.NEED_CHUNK, result);

        result = gen.generateResponse(null, false, null, chunk, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        out += BufferUtils.toString(chunk);
        BufferUtils.clear(chunk);
        out += BufferUtils.toString(content1);
        BufferUtils.clear(content1);

        result = gen.generateResponse(null, false, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, false, null, chunk, null, true);

        assertEquals(HttpGenerator.Result.NEED_CHUNK_TRAILER, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, false, null, trailer, null, true);

        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        out += BufferUtils.toString(trailer);
        BufferUtils.clear(trailer);

        result = gen.generateResponse(null, false, null, trailer, null, true);
        assertEquals(HttpGenerator.Result.SHUTDOWN_OUT, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertTrue(out.contains("HTTP/1.1 200 OK"));
        assertTrue(out.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertFalse(out.contains("Content-Length"));
        assertTrue(out.contains("Transfer-Encoding: chunked"));

        assertTrue(out.endsWith(
                "\r\n\r\nD\r\n" +
                        "Hello World! \r\n" +
                        "2E\r\n" +
                        "The quick brown fox jumped over the lazy dog. \r\n" +
                        "0\r\n" +
                        "T-Name0: T-ValueA\r\n" +
                        "T-Name0: T-ValueB\r\n" +
                        "T-Name1: T-ValueC\r\n" +
                        "\r\n"));
    }

    @Test
    void testResponseWithTrailer() throws Exception {
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
        ByteBuffer trailer = BufferUtils.allocate(4096);
        HttpGenerator gen = new HttpGenerator();
        gen.setPersistent(false);

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), -1);
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);
        info.getFields().add(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED);
        info.setTrailerSupplier(new Supplier<HttpFields>() {
            @Override
            public HttpFields get() {
                HttpFields trailer = new HttpFields();
                trailer.add("T-Name0", "T-ValueA");
                trailer.add("T-Name0", "T-ValueB");
                trailer.add("T-Name1", "T-ValueC");
                return trailer;
            }
        });

        result = gen.generateResponse(info, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, false, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);

        result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_CHUNK_TRAILER, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, false, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.NEED_CHUNK_TRAILER, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, false, null, trailer, null, true);

        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        out += BufferUtils.toString(trailer);
        BufferUtils.clear(trailer);

        result = gen.generateResponse(null, false, null, trailer, null, true);
        assertEquals(HttpGenerator.Result.SHUTDOWN_OUT, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertTrue(out.contains("HTTP/1.1 200 OK"));
        assertTrue(out.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertFalse(out.contains("Content-Length"));
        assertTrue(out.contains("Transfer-Encoding: chunked"));

        assertTrue(out.endsWith(
                "\r\n\r\n" +
                        "0\r\n" +
                        "T-Name0: T-ValueA\r\n" +
                        "T-Name0: T-ValueB\r\n" +
                        "T-Name1: T-ValueC\r\n" +
                        "\r\n"));
    }

    @Test
    void testResponseWithKnownContentLengthFromMetaData() throws Exception {
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer content0 = BufferUtils.toBuffer("Hello World! ");
        ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog. ");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), 59);
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);
        result = gen.generateResponse(info, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, false, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);
        out += BufferUtils.toString(content0);
        BufferUtils.clear(content0);

        result = gen.generateResponse(null, false, null, null, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        out += BufferUtils.toString(content1);
        BufferUtils.clear(content1);

        result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertTrue(out.contains("HTTP/1.1 200 OK"));
        assertTrue(out.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertFalse(out.contains("chunked"));
        assertTrue(out.contains("Content-Length: 59"));
        assertTrue(out.contains("\r\n\r\nHello World! The quick brown fox jumped over the lazy dog. "));
    }

    @Test
    void testResponseWithKnownContentLengthFromHeader() throws Exception {
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer content0 = BufferUtils.toBuffer("Hello World! ");
        ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog. ");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), -1);
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);
        info.getFields().add("Content-Length", "" + (content0.remaining() + content1.remaining()));
        result = gen.generateResponse(info, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, false, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

        String out = BufferUtils.toString(header);
        BufferUtils.clear(header);
        out += BufferUtils.toString(content0);
        BufferUtils.clear(content0);

        result = gen.generateResponse(null, false, null, null, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        out += BufferUtils.toString(content1);
        BufferUtils.clear(content1);

        result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertTrue(out.contains("HTTP/1.1 200 OK"));
        assertTrue(out.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertFalse(out.contains("chunked"));
        assertTrue(out.contains("Content-Length: 59"));
        assertTrue(out.contains("\r\n\r\nHello World! The quick brown fox jumped over the lazy dog. "));
    }


    @Test
    void test100ThenResponseWithContent() throws Exception {
        ByteBuffer header = BufferUtils.allocate(4096);
        ByteBuffer content0 = BufferUtils.toBuffer("Hello World! ");
        ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog. ");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(HttpGenerator.CONTINUE_100_INFO, false, null, null, null, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(HttpGenerator.CONTINUE_100_INFO, false, header, null, null, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING_1XX, gen.getState());
        String out = BufferUtils.toString(header);

        result = gen.generateResponse(null, false, null, null, null, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        assertTrue(out.contains("HTTP/1.1 100 Continue"));

        result = gen.generateResponse(null, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), BufferUtils.length(content0) + BufferUtils.length(content1));
        info.getFields().add("Last-Modified", DateGenerator.JAN_01_1970);
        result = gen.generateResponse(info, false, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, false, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

        out = BufferUtils.toString(header);
        BufferUtils.clear(header);
        out += BufferUtils.toString(content0);
        BufferUtils.clear(content0);

        result = gen.generateResponse(null, false, null, null, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        out += BufferUtils.toString(content1);
        BufferUtils.clear(content1);

        result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, false, null, null, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertTrue(out.contains("HTTP/1.1 200 OK"));
        assertTrue(out.contains("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertFalse(out.contains("chunked"));
        assertTrue(out.contains("Content-Length: 59"));
        assertTrue(out.contains("\r\n\r\nHello World! The quick brown fox jumped over the lazy dog. "));
    }

    @Test
    void testConnectionKeepAliveWithAdditionalCustomValue() throws Exception {
        HttpGenerator generator = new HttpGenerator();

        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE);
        String customValue = "test";
        fields.add(HttpHeader.CONNECTION, customValue);
        MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_0, 200, "OK", fields, -1);
        ByteBuffer header = BufferUtils.allocate(4096);
        HttpGenerator.Result result = generator.generateResponse(info, false, header, null, null, true);
        assertSame(HttpGenerator.Result.FLUSH, result);
        String headers = BufferUtils.toString(header);
        assertTrue(headers.contains(HttpHeaderValue.KEEP_ALIVE.getValue()));
        assertTrue(headers.contains(customValue));
    }
}
