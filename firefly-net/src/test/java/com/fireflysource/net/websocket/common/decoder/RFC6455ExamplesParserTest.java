package com.fireflysource.net.websocket.common.decoder;


import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.websocket.common.frame.Frame;
import com.fireflysource.net.websocket.common.frame.WebSocketFrame;
import com.fireflysource.net.websocket.common.model.IncomingFramesCapture;
import com.fireflysource.net.websocket.common.model.OpCode;
import com.fireflysource.net.websocket.common.model.WebSocketBehavior;
import com.fireflysource.net.websocket.common.model.WebSocketPolicy;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Collection of Example packets as found in <a href="https://tools.ietf.org/html/rfc6455#section-5.7">RFC 6455 Examples section</a>
 */
public class RFC6455ExamplesParserTest {

    @Test
    public void testFragmentedUnmaskedTextMessage() {
        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.CLIENT);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        ByteBuffer buf = ByteBuffer.allocate(16);
        BufferUtils.clearToFill(buf);

        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // A fragmented unmasked text message (part 1 of 2 "Hel")
        buf.put(new byte[]
                {(byte) 0x01, (byte) 0x03, 0x48, (byte) 0x65, 0x6c});

        // Parse #1
        BufferUtils.flipToFlush(buf, 0);
        parser.parse(buf);

        // part 2 of 2 "lo" (A continuation frame of the prior text message)
        BufferUtils.flipToFill(buf);
        buf.put(new byte[]
                {(byte) 0x80, 0x02, 0x6c, 0x6f});

        // Parse #2
        BufferUtils.flipToFlush(buf, 0);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.TEXT, 1);
        capture.assertHasFrame(OpCode.CONTINUATION, 1);

        WebSocketFrame txt = capture.getFrames().poll();
        String actual = BufferUtils.toUTF8String(txt.getPayload());
        assertEquals("Hel", actual);
        txt = capture.getFrames().poll();
        actual = BufferUtils.toUTF8String(txt.getPayload());
        assertEquals("lo", actual);
    }

    @Test
    public void testSingleMaskedPongRequest() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // Unmasked Pong request
        buf.put(new byte[]
                {(byte) 0x8a, (byte) 0x85, 0x37, (byte) 0xfa, 0x21, 0x3d, 0x7f, (byte) 0x9f, 0x4d, 0x51, 0x58});
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.PONG, 1);

        WebSocketFrame pong = capture.getFrames().poll();
        String actual = BufferUtils.toUTF8String(pong.getPayload());
        assertEquals("Hello", actual);
    }

    @Test
    public void testSingleMaskedTextMessage() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // A single-frame masked text message
        buf.put(new byte[]
                {(byte) 0x81, (byte) 0x85, 0x37, (byte) 0xfa, 0x21, 0x3d, 0x7f, (byte) 0x9f, 0x4d, 0x51, 0x58});
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.TEXT, 1);

        WebSocketFrame txt = capture.getFrames().poll();
        String actual = BufferUtils.toUTF8String(txt.getPayload());
        assertEquals("Hello", actual);
    }

    @Test
    public void testSingleUnmasked256ByteBinaryMessage() {
        int dataSize = 256;

        ByteBuffer buf = ByteBuffer.allocate(dataSize + 10);
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // 256 bytes binary message in a single unmasked frame
        buf.put(new byte[]
                {(byte) 0x82, 0x7E});
        buf.putShort((short) 0x01_00); // 16 bit size
        for (int i = 0; i < dataSize; i++) {
            buf.put((byte) 0x44);
        }
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.CLIENT);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.BINARY, 1);

        Frame bin = capture.getFrames().poll();

        assertEquals(dataSize, bin.getPayloadLength());

        ByteBuffer data = bin.getPayload();
        assertEquals(dataSize, data.remaining());

        for (int i = 0; i < dataSize; i++) {
            assertEquals((byte) 0x44, data.get(i));
        }
    }

    @Test
    public void testSingleUnmasked64KByteBinaryMessage() {
        int dataSize = 1024 * 64;

        ByteBuffer buf = ByteBuffer.allocate((dataSize + 10));
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // 64 Kbytes binary message in a single unmasked frame
        buf.put(new byte[]
                {(byte) 0x82, 0x7F});
        buf.putLong(dataSize); // 64bit size
        for (int i = 0; i < dataSize; i++) {
            buf.put((byte) 0x77);
        }
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.CLIENT);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.BINARY, 1);

        Frame bin = capture.getFrames().poll();

        assertEquals(dataSize, bin.getPayloadLength());
        ByteBuffer data = bin.getPayload();
        assertEquals(dataSize, data.remaining());

        for (int i = 0; i < dataSize; i++) {
            assertEquals((byte) 0x77, data.get(i));
        }
    }

    @Test
    public void testSingleUnmaskedPingRequest() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // Unmasked Ping request
        buf.put(new byte[]
                {(byte) 0x89, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f});
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.CLIENT);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.PING, 1);

        WebSocketFrame ping = capture.getFrames().poll();
        String actual = BufferUtils.toUTF8String(ping.getPayload());
        assertEquals("Hello", actual);
    }

    @Test
    public void testSingleUnmaskedTextMessage() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        // Raw bytes as found in RFC 6455, Section 5.7 - Examples
        // A single-frame unmasked text message
        buf.put(new byte[]
                {(byte) 0x81, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f});
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.CLIENT);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.TEXT, 1);

        WebSocketFrame txt = capture.getFrames().poll();
        String actual = BufferUtils.toUTF8String(txt.getPayload());
        assertEquals("Hello", actual);
    }
}
