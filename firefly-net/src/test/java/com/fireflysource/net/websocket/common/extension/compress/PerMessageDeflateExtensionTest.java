package com.fireflysource.net.websocket.common.extension.compress;


import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.object.TypeUtils;
import com.fireflysource.net.websocket.common.exception.ProtocolException;
import com.fireflysource.net.websocket.common.extension.AbstractExtensionTest;
import com.fireflysource.net.websocket.common.extension.ExtensionTool.Tester;
import com.fireflysource.net.websocket.common.frame.*;
import com.fireflysource.net.websocket.common.model.*;
import com.fireflysource.net.websocket.common.utils.ByteBufferAssert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Client side behavioral tests for permessage-deflate extension.
 * <p>
 * See: http://tools.ietf.org/html/draft-ietf-hybi-permessage-compression-15
 */
public class PerMessageDeflateExtensionTest extends AbstractExtensionTest {

    private void assertEndsWithTail(String hexStr, boolean expectedResult) {
        ByteBuffer buf = ByteBuffer.wrap(TypeUtils.fromHexString(hexStr));
        assertEquals(expectedResult, CompressExtension.endsWithTail(buf));
    }

    @Test
    public void testEndsWithTailBytes() {
        assertEndsWithTail("11223344", false);
        assertEndsWithTail("00", false);
        assertEndsWithTail("0000", false);
        assertEndsWithTail("FFFF0000", false);
        assertEndsWithTail("880000FFFF", true);
        assertEndsWithTail("0000FFFF", true);
    }

    /**
     * Decode payload example as seen in draft-ietf-hybi-permessage-compression-21.
     * <p>
     * Section 8.2.3.1: A message compressed using 1 compressed DEFLATE block
     */
    @Test
    public void testDraft21HelloUnCompressedBlock() {
        Tester tester = clientExtensions.newTester("permessage-deflate");

        tester.assertNegotiated("permessage-deflate");

        tester.parseIncomingHex(
                // basic, 1 block, compressed with 0 compression level (aka, uncompressed).
                "0xc1 0x07",  // (HEADER added for this test)
                "0xf2 0x48 0xcd 0xc9 0xc9 0x07 0x00" // example frame from RFC
        );

        tester.assertHasFrames("Hello");
    }

    /**
     * Decode payload example as seen in draft-ietf-hybi-permessage-compression-21.
     * <p>
     * Section 8.2.3.1: A message compressed using 1 compressed DEFLATE block (with fragmentation)
     */
    @Test
    public void testDraft21HelloUnCompressedBlockFragmented() {
        Tester tester = clientExtensions.newTester("permessage-deflate");

        tester.assertNegotiated("permessage-deflate");

        tester.parseIncomingHex(// basic, 1 block, compressed with 0 compression level (aka, uncompressed).
                // Fragment 1
                "0x41 0x03 0xf2 0x48 0xcd",
                // Fragment 2
                "0x80 0x04 0xc9 0xc9 0x07 0x00");

        tester.assertHasFrames(
                new TextFrame().setPayload("He").setFin(false),
                new ContinuationFrame().setPayload("llo").setFin(true));
    }

    /**
     * Decode payload example as seen in draft-ietf-hybi-permessage-compression-21.
     * <p>
     * Section 8.2.3.2: Sharing LZ77 Sliding Window
     */
    @Test
    public void testDraft21SharingL77SlidingWindowContextTakeover() {
        Tester tester = clientExtensions.newTester("permessage-deflate");

        tester.assertNegotiated("permessage-deflate");

        tester.parseIncomingHex(// context takeover (2 messages)
                // message 1
                "0xc1 0x07", // (HEADER added for this test)
                "0xf2 0x48 0xcd 0xc9 0xc9 0x07 0x00",
                // message 2
                "0xc1 0x07", // (HEADER added for this test)
                "0xf2 0x48 0xcd 0xc9 0xc9 0x07 0x00");

        tester.assertHasFrames("Hello", "Hello");
    }

    /**
     * Decode payload example as seen in draft-ietf-hybi-permessage-compression-21.
     * <p>
     * Section 8.2.3.2: Sharing LZ77 Sliding Window
     */
    @Test
    public void testDraft21SharingL77SlidingWindowNoContextTakeover() {
        Tester tester = clientExtensions.newTester("permessage-deflate");

        tester.assertNegotiated("permessage-deflate");

        tester.parseIncomingHex(// 2 message, shared LZ77 window
                // message 1
                "0xc1 0x07", // (HEADER added for this test)
                "0xf2 0x48 0xcd 0xc9 0xc9 0x07 0x00",
                // message 2
                "0xc1 0x05", // (HEADER added for this test)
                "0xf2 0x00 0x11 0x00 0x00"
        );

        tester.assertHasFrames("Hello", "Hello");
    }

    /**
     * Decode payload example as seen in draft-ietf-hybi-permessage-compression-21.
     * <p>
     * Section 8.2.3.3: Using a DEFLATE Block with No Compression
     */
    @Test
    public void testDraft21DeflateBlockWithNoCompression() {
        Tester tester = clientExtensions.newTester("permessage-deflate");

        tester.assertNegotiated("permessage-deflate");

        tester.parseIncomingHex(// 1 message / no compression
                "0xc1 0x0b 0x00 0x05 0x00 0xfa 0xff 0x48 0x65 0x6c 0x6c 0x6f 0x00" // example frame
        );

        tester.assertHasFrames("Hello");
    }

    /**
     * Decode payload example as seen in draft-ietf-hybi-permessage-compression-21.
     * <p>
     * Section 8.2.3.4: Using a DEFLATE Block with BFINAL Set to 1
     */
    @Test
    public void testDraft21DeflateBlockWithBFinal1() {
        Tester tester = clientExtensions.newTester("permessage-deflate");

        tester.assertNegotiated("permessage-deflate");

        tester.parseIncomingHex(// 1 message
                "0xc1 0x08", // header
                "0xf3 0x48 0xcd 0xc9 0xc9 0x07 0x00 0x00" // example payload
        );

        tester.assertHasFrames("Hello");
    }

    /**
     * Decode payload example as seen in draft-ietf-hybi-permessage-compression-21.
     * <p>
     * Section 8.2.3.5: Two DEFLATE Blocks in 1 Message
     */
    @Test
    public void testDraft21TwoDeflateBlocksOneMessage() {
        Tester tester = clientExtensions.newTester("permessage-deflate");

        tester.assertNegotiated("permessage-deflate");

        tester.parseIncomingHex(// 1 message, 1 frame, 2 deflate blocks
                "0xc1 0x0d", // (HEADER added for this test)
                "0xf2 0x48 0x05 0x00 0x00 0x00 0xff 0xff 0xca 0xc9 0xc9 0x07 0x00"
        );

        tester.assertHasFrames("Hello");
    }

    /**
     * Decode fragmented message (3 parts: TEXT, CONTINUATION, CONTINUATION)
     */
    @Test
    public void testParseFragmentedMessageGood() {
        Tester tester = clientExtensions.newTester("permessage-deflate");

        tester.assertNegotiated("permessage-deflate");

        tester.parseIncomingHex(// 1 message, 3 frame
                "410C", // HEADER TEXT / fin=false / rsv1=true
                "F248CDC9C95700000000FFFF",
                "000B", // HEADER CONTINUATION / fin=false / rsv1=false
                "0ACF2FCA4901000000FFFF",
                "8003", // HEADER CONTINUATION / fin=true / rsv1=false
                "520400"
        );

        Frame txtFrame = new TextFrame().setPayload("Hello ").setFin(false);
        Frame con1Frame = new ContinuationFrame().setPayload("World").setFin(false);
        Frame con2Frame = new ContinuationFrame().setPayload("!").setFin(true);

        tester.assertHasFrames(txtFrame, con1Frame, con2Frame);
    }

    /**
     * Decode fragmented message (3 parts: TEXT, CONTINUATION, CONTINUATION)
     * <p>
     * Continuation frames have RSV1 set, which MUST result in Failure
     * </p>
     */
    @Test
    public void testParseFragmentedMessageBadRsv1() {
        Tester tester = clientExtensions.newTester("permessage-deflate");

        tester.assertNegotiated("permessage-deflate");

        assertThrows(ProtocolException.class, () ->
                tester.parseIncomingHex(// 1 message, 3 frame
                        "410C", // Header TEXT / fin=false / rsv1=true
                        "F248CDC9C95700000000FFFF", // Payload
                        "400B", // Header CONTINUATION / fin=false / rsv1=true
                        "0ACF2FCA4901000000FFFF", // Payload
                        "C003", // Header CONTINUATION / fin=true / rsv1=true
                        "520400" // Payload
                ));
    }

    /**
     * Incoming PING (Control Frame) should pass through extension unmodified
     */
    @Test
    public void testIncomingPing() {
        PerMessageDeflateExtension ext = new PerMessageDeflateExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("permessage-deflate");
        ext.setConfig(config);

        // Setup capture of incoming frames
        IncomingFramesCapture capture = new IncomingFramesCapture();

        // Wire up stack
        ext.setNextIncomingFrames(capture);

        String payload = "Are you there?";
        Frame ping = new PingFrame().setPayload(payload);
        ext.incomingFrame(ping);

        capture.assertFrameCount(1);
        capture.assertHasFrame(OpCode.PING, 1);
        WebSocketFrame actual = capture.getFrames().poll();

        assertEquals(OpCode.PING, actual.getOpCode());
        assertTrue(actual.isFin());
        assertFalse(actual.isRsv1());
        assertFalse(actual.isRsv2());
        assertFalse(actual.isRsv3());

        ByteBuffer expected = BufferUtils.toBuffer(payload, StandardCharsets.UTF_8);
        assertEquals(expected.remaining(), actual.getPayloadLength());
        ByteBufferAssert.assertEquals("Frame.payload", expected, actual.getPayload().slice());
    }

    /**
     * Incoming Text Message fragmented into 3 pieces.
     */
    @Test
    public void testIncomingFragmented() {
        PerMessageDeflateExtension ext = new PerMessageDeflateExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("permessage-deflate");
        ext.setConfig(config);

        // Setup capture of incoming frames
        IncomingFramesCapture capture = new IncomingFramesCapture();

        // Wire up stack
        ext.setNextIncomingFrames(capture);

        String payload = "Are you there?";
        Frame ping = new PingFrame().setPayload(payload);
        ext.incomingFrame(ping);

        capture.assertFrameCount(1);
        capture.assertHasFrame(OpCode.PING, 1);
        WebSocketFrame actual = capture.getFrames().poll();

        assertEquals(OpCode.PING, actual.getOpCode());
        assertTrue(actual.isFin());
        assertFalse(actual.isRsv1());
        assertFalse(actual.isRsv2());
        assertFalse(actual.isRsv3());

        ByteBuffer expected = BufferUtils.toBuffer(payload, StandardCharsets.UTF_8);
        assertEquals(expected.remaining(), actual.getPayloadLength());
        ByteBufferAssert.assertEquals("Frame.payload", expected, actual.getPayload().slice());
    }

    /**
     * Verify that incoming uncompressed frames are properly passed through
     */
    @Test
    public void testIncomingUncompressedFrames() {
        PerMessageDeflateExtension ext = new PerMessageDeflateExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("permessage-deflate");
        ext.setConfig(config);

        // Setup capture of incoming frames
        IncomingFramesCapture capture = new IncomingFramesCapture();

        // Wire up stack
        ext.setNextIncomingFrames(capture);

        // Quote
        List<String> quote = new ArrayList<>();
        quote.add("No amount of experimentation can ever prove me right;");
        quote.add("a single experiment can prove me wrong.");
        quote.add("-- Albert Einstein");

        // leave frames as-is, no compression, and pass into extension
        for (String q : quote) {
            TextFrame frame = new TextFrame().setPayload(q);
            frame.setRsv1(false); // indication to extension that frame is not compressed (ie: a normal frame)
            ext.incomingFrame(frame);
        }

        int len = quote.size();
        capture.assertFrameCount(len);
        capture.assertHasFrame(OpCode.TEXT, len);

        String prefix;
        int i = 0;
        for (WebSocketFrame actual : capture.getFrames()) {
            prefix = "Frame[" + i + "]";

            assertEquals(OpCode.TEXT, actual.getOpCode());
            assertTrue(actual.isFin());
            assertFalse(actual.isRsv1());
            assertFalse(actual.isRsv2());
            assertFalse(actual.isRsv3());

            ByteBuffer expected = BufferUtils.toBuffer(quote.get(i), StandardCharsets.UTF_8);
            assertEquals(expected.remaining(), actual.getPayloadLength());
            ByteBufferAssert.assertEquals(prefix + ".payload", expected, actual.getPayload().slice());
            i++;
        }
    }

    /**
     * Outgoing PING (Control Frame) should pass through extension unmodified
     *
     * @throws IOException on test failure
     */
    @Test
    public void testOutgoingPing() throws IOException {
        PerMessageDeflateExtension ext = new PerMessageDeflateExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("permessage-deflate");
        ext.setConfig(config);

        // Setup capture of outgoing frames
        OutgoingFramesCapture capture = new OutgoingFramesCapture();

        // Wire up stack
        ext.setNextOutgoingFrames(capture);

        String payload = "Are you there?";
        Frame ping = new PingFrame().setPayload(payload);

        ext.outgoingFrame(ping, null);

        capture.assertFrameCount(1);
        capture.assertHasFrame(OpCode.PING, 1);

        WebSocketFrame actual = capture.getFrames().getFirst();

        assertEquals(OpCode.PING, actual.getOpCode());
        assertTrue(actual.isFin());
        assertFalse(actual.isRsv1());
        assertFalse(actual.isRsv2());
        assertFalse(actual.isRsv3());

        ByteBuffer expected = BufferUtils.toBuffer(payload, StandardCharsets.UTF_8);
        assertEquals(expected.remaining(), actual.getPayloadLength());
        ByteBufferAssert.assertEquals("Frame.payload", expected, actual.getPayload().slice());
    }

    /**
     * Outgoing Fragmented Message
     *
     * @throws IOException on test failure
     */
    @Test
    public void testOutgoingFragmentedMessage() throws InterruptedException {
        PerMessageDeflateExtension ext = new PerMessageDeflateExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("permessage-deflate");
        ext.setConfig(config);

        // Setup capture of outgoing frames
        OutgoingFramesCapture capture = new OutgoingFramesCapture();

        // Wire up stack
        ext.setNextOutgoingFrames(capture);

        Frame txtFrame = new TextFrame().setPayload("Hello ").setFin(false);
        Frame con1Frame = new ContinuationFrame().setPayload("World").setFin(false);
        Frame con2Frame = new ContinuationFrame().setPayload("!").setFin(true);
        ext.outgoingFrame(txtFrame, null);
        ext.outgoingFrame(con1Frame, null);
        ext.outgoingFrame(con2Frame, null);

        capture.assertFrameCount(3);

        WebSocketFrame capturedFrame;

        capturedFrame = capture.getFrames().poll(1, TimeUnit.SECONDS);
        assertEquals(OpCode.TEXT, capturedFrame.getOpCode());
        assertFalse(capturedFrame.isFin());
        assertTrue(capturedFrame.isRsv1());
        assertFalse(capturedFrame.isRsv2());
        assertFalse(capturedFrame.isRsv3());

        capturedFrame = capture.getFrames().poll(1, TimeUnit.SECONDS);
        assertEquals(OpCode.CONTINUATION, capturedFrame.getOpCode());
        assertFalse(capturedFrame.isFin());
        assertFalse(capturedFrame.isRsv1());
        assertFalse(capturedFrame.isRsv2());
        assertFalse(capturedFrame.isRsv3());

        capturedFrame = capture.getFrames().poll(1, TimeUnit.SECONDS);
        assertEquals(OpCode.CONTINUATION, capturedFrame.getOpCode());
        assertTrue(capturedFrame.isFin());
        assertFalse(capturedFrame.isRsv1());
        assertFalse(capturedFrame.isRsv2());
        assertFalse(capturedFrame.isRsv3());
    }

    @Test
    public void testPyWebSocketClientNoContextTakeoverThreeOra() {
        Tester tester = clientExtensions.newTester("permessage-deflate; client_max_window_bits; client_no_context_takeover");

        tester.assertNegotiated("permessage-deflate");

        // Captured from Pywebsocket (r790) - 3 messages with similar parts.

        tester.parseIncomingHex(// context takeover (3 messages)
                "c1 09 0a c9 2f 4a 0c 01  62 00 00", // ToraTora
                "c1 0b 72 2c c9 2f 4a 74  cb 01 12 00 00", // AtoraFlora
                "c1 0b 0a c8 c8 c9 2f 4a  0c 01 62 00 00" // PhloraTora
        );

        tester.assertHasFrames("ToraTora", "AtoraFlora", "PhloraTora");
    }

    @Test
    public void testPyWebSocketClientToraToraTora() {
        Tester tester = clientExtensions.newTester("permessage-deflate; client_max_window_bits");

        tester.assertNegotiated("permessage-deflate");

        // Captured from Pywebsocket (r790) - "tora" sent 3 times.

        tester.parseIncomingHex(// context takeover (3 messages)
                "c1 06 2a c9 2f 4a 04 00", // tora 1
                "c1 05 2a 01 62 00 00", // tora 2
                "c1 04 02 61 00 00" // tora 3
        );

        tester.assertHasFrames("tora", "tora", "tora");
    }

    @Test
    public void testPyWebSocketServerNoContextTakeoverThreeOra() {
        Tester tester = serverExtensions.newTester("permessage-deflate; client_max_window_bits; client_no_context_takeover");

        tester.assertNegotiated("permessage-deflate");

        // Captured from Pywebsocket (r790) - 3 messages with similar parts.

        tester.parseIncomingHex(// context takeover (3 messages)
                "c1 89 88 bc 1b b1 82 75  34 fb 84 bd 79 b1 88", // ToraTora
                "c1 8b 50 86 88 b2 22 aa  41 9d 1a f2 43 b3 42 86 88", // AtoraFlora
                "c1 8b e2 3e 05 53 e8 f6  cd 9a cd 74 09 52 80 3e 05" // PhloraTora
        );

        tester.assertHasFrames("ToraTora", "AtoraFlora", "PhloraTora");
    }

    @Test
    public void testPyWebSocketServerToraToraTora() {
        Tester tester = serverExtensions.newTester("permessage-deflate; client_max_window_bits");

        tester.assertNegotiated("permessage-deflate");

        // Captured from Pywebsocket (r790) - "tora" sent 3 times.

        tester.parseIncomingHex(// context takeover (3 messages)
                "c1 86 69 39 fe 91 43 f0  d1 db 6d 39", // tora 1
                "c1 85 2d f3 eb 96 07 f2  89 96 2d", // tora 2
                "c1 84 53 ad a5 34 51 cc  a5 34" // tora 3
        );

        tester.assertHasFrames("tora", "tora", "tora");
    }
}
