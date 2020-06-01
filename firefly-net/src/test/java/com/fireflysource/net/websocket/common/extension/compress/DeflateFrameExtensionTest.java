package com.fireflysource.net.websocket.common.extension.compress;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.object.TypeUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.Result;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.websocket.common.decoder.Parser;
import com.fireflysource.net.websocket.common.decoder.UnitParser;
import com.fireflysource.net.websocket.common.encoder.Generator;
import com.fireflysource.net.websocket.common.extension.AbstractExtensionTest;
import com.fireflysource.net.websocket.common.extension.ExtensionTool.Tester;
import com.fireflysource.net.websocket.common.frame.BinaryFrame;
import com.fireflysource.net.websocket.common.frame.Frame;
import com.fireflysource.net.websocket.common.frame.TextFrame;
import com.fireflysource.net.websocket.common.frame.WebSocketFrame;
import com.fireflysource.net.websocket.common.model.*;
import com.fireflysource.net.websocket.common.utils.ByteBufferAssert;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.*;

public class DeflateFrameExtensionTest extends AbstractExtensionTest {
    private static final LazyLogger LOG = SystemLogger.create(DeflateFrameExtensionTest.class);

    private void assertIncoming(byte[] raw, String... expectedTextDatas) {
        WebSocketPolicy policy = WebSocketPolicy.newClientPolicy();

        DeflateFrameExtension ext = new DeflateFrameExtension();
        ext.setPolicy(policy);

        ExtensionConfig config = ExtensionConfig.parse("deflate-frame");
        ext.setConfig(config);

        // Setup capture of incoming frames
        IncomingFramesCapture capture = new IncomingFramesCapture();

        // Wire up stack
        ext.setNextIncomingFrames(capture);

        Parser parser = new UnitParser(policy);
        parser.configureFromExtensions(Collections.singletonList(ext));
        parser.setIncomingFramesHandler(ext);

        parser.parse(ByteBuffer.wrap(raw));

        int len = expectedTextDatas.length;
        capture.assertFrameCount(len);
        capture.assertHasFrame(OpCode.TEXT, len);

        int i = 0;
        for (WebSocketFrame actual : capture.getFrames()) {
            String prefix = "Frame[" + i + "]";
            assertEquals(OpCode.TEXT, actual.getOpCode());
            assertTrue(actual.isFin());
            assertFalse(actual.isRsv1());
            assertFalse(actual.isRsv2());
            assertFalse(actual.isRsv3());

            ByteBuffer expected = BufferUtils.toBuffer(expectedTextDatas[i], StandardCharsets.UTF_8);
            assertEquals(expected.remaining(), actual.getPayloadLength());
            ByteBufferAssert.assertEquals(prefix + ".payload", expected, actual.getPayload().slice());
            i++;
        }
    }

    private void assertOutgoing(String text, String expectedHex) throws IOException {
        WebSocketPolicy policy = WebSocketPolicy.newClientPolicy();

        DeflateFrameExtension ext = new DeflateFrameExtension();
        ext.setPolicy(policy);

        ExtensionConfig config = ExtensionConfig.parse("deflate-frame");
        ext.setConfig(config);

        Generator generator = new Generator(policy, true);
        generator.configureFromExtensions(Collections.singletonList(ext));

        OutgoingNetworkBytesCapture capture = new OutgoingNetworkBytesCapture(generator);
        ext.setNextOutgoingFrames(capture);

        Frame frame = new TextFrame().setPayload(text);
        ext.outgoingFrame(frame, null);

        capture.assertBytes(0, expectedHex);
    }

    @Test
    public void testBlockheadClientHelloThere() {
        Tester tester = serverExtensions.newTester("deflate-frame");

        tester.assertNegotiated("deflate-frame");

        tester.parseIncomingHex(// Captured from Blockhead Client - "Hello" then "There" via unit test
                "c18700000000f248cdc9c90700", // "Hello"
                "c187000000000ac9482d4a0500" // "There"
        );

        tester.assertHasFrames("Hello", "There");
    }

    @Test
    public void testChrome20Hello() {
        Tester tester = serverExtensions.newTester("deflate-frame");

        tester.assertNegotiated("deflate-frame");

        tester.parseIncomingHex(// Captured from Chrome 20.x - "Hello" (sent from browser)
                "c187832b5c11716391d84a2c5c" // "Hello"
        );

        tester.assertHasFrames("Hello");
    }

    @Test
    public void testChrome20HelloThere() {
        Tester tester = serverExtensions.newTester("deflate-frame");

        tester.assertNegotiated("deflate-frame");

        tester.parseIncomingHex(// Captured from Chrome 20.x - "Hello" then "There" (sent from browser)
                "c1877b1971db8951bc12b21e71", // "Hello"
                "c18759edc8f4532480d913e8c8" // There
        );

        tester.assertHasFrames("Hello", "There");
    }

    @Test
    public void testChrome20Info() {
        Tester tester = serverExtensions.newTester("deflate-frame");

        tester.assertNegotiated("deflate-frame");

        tester.parseIncomingHex(// Captured from Chrome 20.x - "info:" (sent from browser)
                "c187ca4def7f0081a4b47d4fef" // example payload
        );

        tester.assertHasFrames("info:");
    }

    @Test
    public void testChrome20TimeTime() {
        Tester tester = serverExtensions.newTester("deflate-frame");

        tester.assertNegotiated("deflate-frame");

        tester.parseIncomingHex(// Captured from Chrome 20.x - "time:" then "time:" once more (sent from browser)
                "c18782467424a88fb869374474", // "time:"
                "c1853cfda17f16fcb07f3c" // "time:"
        );

        tester.assertHasFrames("time:", "time:");
    }

    @Test
    public void testPyWebSocketTimeTimeTime() {
        Tester tester = serverExtensions.newTester("deflate-frame");

        tester.assertNegotiated("deflate-frame");

        tester.parseIncomingHex(// Captured from Pywebsocket (r781) - "time:" sent 3 times.
                "c1876b100104" + "41d9cd49de1201", // "time:"
                "c1852ae3ff01" + "00e2ee012a", // "time:"
                "c18435558caa" + "37468caa" // "time:"
        );

        tester.assertHasFrames("time:", "time:", "time:");
    }

    @Test
    public void testCompressTimeTimeTime() {
        // What pywebsocket produces for "time:", "time:", "time:"
        String[] expected = new String[]
                {"2AC9CC4DB50200", "2A01110000", "02130000"};

        // Lets see what we produce
        CapturedHexPayloads capture = new CapturedHexPayloads();
        DeflateFrameExtension ext = new DeflateFrameExtension();
        init(ext);
        ext.setNextOutgoingFrames(capture);

        ext.outgoingFrame(new TextFrame().setPayload("time:"), null);
        ext.outgoingFrame(new TextFrame().setPayload("time:"), null);
        ext.outgoingFrame(new TextFrame().setPayload("time:"), null);

        List<String> actual = capture.getCaptured();
        assertTrue(actual.containsAll(Arrays.asList(expected)));
    }

    private void init(DeflateFrameExtension ext) {
        ext.setConfig(new ExtensionConfig(ext.getName()));
    }

    @Test
    public void testDeflateBasics() {
        // Setup deflater basics
        Deflater compressor = new Deflater(Deflater.BEST_COMPRESSION, true);
        compressor.setStrategy(Deflater.DEFAULT_STRATEGY);

        // Text to compress
        String text = "info:";
        byte[] uncompressed = StringUtils.getUtf8Bytes(text);

        // Prime the compressor
        compressor.reset();
        compressor.setInput(uncompressed, 0, uncompressed.length);
        compressor.finish();

        // Perform compression
        ByteBuffer outbuf = ByteBuffer.allocate(64);
        BufferUtils.clearToFill(outbuf);

        while (!compressor.finished()) {
            byte[] out = new byte[64];
            int len = compressor.deflate(out, 0, out.length, Deflater.SYNC_FLUSH);
            if (len > 0) {
                outbuf.put(out, 0, len);
            }
        }
        compressor.end();

        BufferUtils.flipToFlush(outbuf, 0);
        byte[] compressed = BufferUtils.toArray(outbuf);
        // Clear the BFINAL bit that has been set by the compressor.end() call.
        // In the real implementation we never end() the compressor.
        compressed[0] &= 0xFE;

        String actual = TypeUtils.toHexString(compressed);
        String expected = "CaCc4bCbB70200"; // what pywebsocket produces

        assertEquals(expected, actual);
    }

    @Test
    public void testGeneratedTwoFrames() throws IOException {
        WebSocketPolicy policy = WebSocketPolicy.newClientPolicy();

        DeflateFrameExtension ext = new DeflateFrameExtension();
        ext.setPolicy(policy);
        ext.setConfig(new ExtensionConfig(ext.getName()));

        Generator generator = new Generator(policy, true);
        generator.configureFromExtensions(Collections.singletonList(ext));

        OutgoingNetworkBytesCapture capture = new OutgoingNetworkBytesCapture(generator);
        ext.setNextOutgoingFrames(capture);

        ext.outgoingFrame(new TextFrame().setPayload("Hello"), null);
        ext.outgoingFrame(new TextFrame(), null);
        ext.outgoingFrame(new TextFrame().setPayload("There"), null);

        capture.assertBytes(0, "c107f248cdc9c90700");
    }

    @Test
    public void testInflateBasics() throws Exception {
        // should result in "info:" text if properly inflated
        byte[] rawbuf = TypeUtils.fromHexString("CaCc4bCbB70200"); // what pywebsocket produces
        // byte[] rawbuf = TypeUtil.fromHexString("CbCc4bCbB70200"); // what java produces

        Inflater inflater = new Inflater(true);
        inflater.reset();
        inflater.setInput(rawbuf, 0, rawbuf.length);

        byte[] outbuf = new byte[64];
        int len = inflater.inflate(outbuf);
        inflater.end();
        assertTrue(len > 4);

        String actual = new String(outbuf, 0, len, StandardCharsets.UTF_8);
        assertEquals("info:", actual);
    }

    @Test
    public void testPyWebSocketServerHello() {
        // Captured from PyWebSocket - "Hello" (echo from server)
        byte[] rawbuf = TypeUtils.fromHexString("c107f248cdc9c90700");
        assertIncoming(rawbuf, "Hello");
    }

    @Test
    public void testPyWebSocketServerLong() {
        // Captured from PyWebSocket - Long Text (echo from server)
        byte[] rawbuf = TypeUtils.fromHexString("c1421cca410a80300c44d1abccce9df7" +
                "f018298634d05631138ab7b7b8fdef1f" +
                "dc0282e2061d575a45f6f2686bab25e1" +
                "3fb7296fa02b5885eb3b0379c394f461" +
                "98cafd03");
        assertIncoming(rawbuf, "It's a big enough umbrella but it's always me that ends up getting wet.");
    }

    @Test
    public void testPyWebSocketServerMedium() {
        // Captured from PyWebSocket - "stackoverflow" (echo from server)
        byte[] rawbuf = TypeUtils.fromHexString("c10f2a2e494ccece2f4b2d4acbc92f0700");
        assertIncoming(rawbuf, "stackoverflow");
    }

    /**
     * Make sure that the server generated compressed form for "Hello" is consistent with what PyWebSocket creates.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testServerGeneratedHello() throws IOException {
        assertOutgoing("Hello", "c107f248cdc9c90700");
    }

    /**
     * Make sure that the server generated compressed form for "There" is consistent with what PyWebSocket creates.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testServerGeneratedThere() throws IOException {
        assertOutgoing("There", "c1070ac9482d4a0500");
    }

    @Test
    public void testCompressAndDecompressBigPayload() throws Exception {
        byte[] input = new byte[1024 * 1024];
        // Make them not compressible.
        new Random().nextBytes(input);

        int maxMessageSize = (1024 * 1024) + 8192;

        DeflateFrameExtension clientExtension = new DeflateFrameExtension();
        clientExtension.setPolicy(WebSocketPolicy.newClientPolicy());
        clientExtension.getPolicy().setMaxBinaryMessageSize(maxMessageSize);
        clientExtension.getPolicy().setMaxBinaryMessageBufferSize(maxMessageSize);
        clientExtension.setConfig(ExtensionConfig.parse("deflate-frame"));

        final DeflateFrameExtension serverExtension = new DeflateFrameExtension();
        serverExtension.setPolicy(WebSocketPolicy.newServerPolicy());
        serverExtension.getPolicy().setMaxBinaryMessageSize(maxMessageSize);
        serverExtension.getPolicy().setMaxBinaryMessageBufferSize(maxMessageSize);
        serverExtension.setConfig(ExtensionConfig.parse("deflate-frame"));

        // Chain the next element to decompress.
        clientExtension.setNextOutgoingFrames(new OutgoingFrames() {
            @Override
            public void outgoingFrame(Frame frame, Consumer<Result<Void>> result) {
                LOG.debug("outgoingFrame({})", frame);
                serverExtension.incomingFrame(frame);
                result.accept(Result.SUCCESS);
            }
        });

        final ByteArrayOutputStream result = new ByteArrayOutputStream(input.length);
        serverExtension.setNextIncomingFrames(new IncomingFrames() {
            @Override
            public void incomingFrame(Frame frame) {
                LOG.debug("incomingFrame({})", frame);
                try {
                    result.write(BufferUtils.toArray(frame.getPayload()));
                } catch (IOException x) {
                    throw new RuntimeException(x);
                }
            }
        });

        BinaryFrame frame = new BinaryFrame();
        frame.setPayload(input);
        frame.setFin(true);
        clientExtension.outgoingFrame(frame, null);

        assertArrayEquals(input, result.toByteArray());
    }
}
