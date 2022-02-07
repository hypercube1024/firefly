package com.fireflysource.net.websocket.common.decoder;

import com.fireflysource.net.websocket.common.exception.MessageTooLargeException;
import com.fireflysource.net.websocket.common.frame.WebSocketFrame;
import com.fireflysource.net.websocket.common.model.IncomingFramesCapture;
import com.fireflysource.net.websocket.common.model.OpCode;
import com.fireflysource.net.websocket.common.model.WebSocketBehavior;
import com.fireflysource.net.websocket.common.model.WebSocketPolicy;
import com.fireflysource.net.websocket.common.utils.MaskedByteBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TextPayloadParserTest {

    @Test
    public void testFrameTooLargeDueToPolicy() throws Exception {
        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        // Artificially small buffer/payload
        policy.setInputBufferSize(1024); // read buffer
        policy.setMaxTextMessageBufferSize(1024); // streaming buffer (not used in this test)
        policy.setMaxTextMessageSize(1024); // actual maximum text message size policy
        byte[] utf = new byte[2048];
        Arrays.fill(utf, (byte) 'a');

        assertTrue(utf.length > 0x7E && utf.length < 0xFFFF);

        ByteBuffer buf = ByteBuffer.allocate(utf.length + 8);
        buf.put((byte) 0x81); // text frame, fin = true
        buf.put((byte) (0x80 | 0x7E)); // 0x7E == 126 (a 2 byte payload length)
        buf.putShort((short) utf.length);
        MaskedByteBuffer.putMask(buf);
        MaskedByteBuffer.putPayload(buf, utf);
        buf.flip();

        UnitParser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        assertThrows(MessageTooLargeException.class, () -> parser.parseQuietly(buf));
    }

    @Test
    public void testLongMaskedText() throws Exception {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 3500; i++) {
            sb.append("Hell\uFF4f Big W\uFF4Frld ");
        }
        sb.append(". The end.");

        String expectedText = sb.toString();
        byte[] utf = expectedText.getBytes(StandardCharsets.UTF_8);

        assertTrue(utf.length > 0xFFFF);

        ByteBuffer buf = ByteBuffer.allocate(utf.length + 32);
        buf.put((byte) 0x81); // text frame, fin = true
        buf.put((byte) (0x80 | 0x7F)); // 0x7F == 127 (a 8 byte payload length)
        buf.putLong(utf.length);
        MaskedByteBuffer.putMask(buf);
        MaskedByteBuffer.putPayload(buf, utf);
        buf.flip();

        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();
        policy.setMaxTextMessageSize(100000);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.TEXT, 1);
        WebSocketFrame txt = capture.getFrames().poll();
        assertEquals(expectedText, txt.getPayloadAsUTF8());
    }

    @Test
    public void testMediumMaskedText() throws Exception {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < 14; i++) {
            sb.append("Hell\uFF4f Medium W\uFF4Frld ");
        }
        sb.append(". The end.");

        String expectedText = sb.toString();
        byte[] utf = expectedText.getBytes(StandardCharsets.UTF_8);

        assertTrue(utf.length > 0x7E && utf.length < 0xFFFF);

        ByteBuffer buf = ByteBuffer.allocate(utf.length + 10);
        buf.put((byte) 0x81);
        buf.put((byte) (0x80 | 0x7E)); // 0x7E == 126 (a 2 byte payload length)
        buf.putShort((short) utf.length);
        MaskedByteBuffer.putMask(buf);
        MaskedByteBuffer.putPayload(buf, utf);
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.TEXT, 1);
        WebSocketFrame txt = capture.getFrames().poll();
        assertEquals(expectedText, txt.getPayloadAsUTF8());
    }

    @Test
    public void testShortMaskedFragmentedText() throws Exception {
        String part1 = "Hello ";
        String part2 = "World";

        byte[] b1 = part1.getBytes(StandardCharsets.UTF_8);
        byte[] b2 = part2.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(32);

        // part 1
        buf.put((byte) 0x01); // no fin + text
        buf.put((byte) (0x80 | b1.length));
        MaskedByteBuffer.putMask(buf);
        MaskedByteBuffer.putPayload(buf, b1);

        // part 2
        buf.put((byte) 0x80); // fin + continuation
        buf.put((byte) (0x80 | b2.length));
        MaskedByteBuffer.putMask(buf);
        MaskedByteBuffer.putPayload(buf, b2);

        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.TEXT, 1);
        capture.assertHasFrame(OpCode.CONTINUATION, 1);
        WebSocketFrame txt = capture.getFrames().poll();
        assertEquals(part1, txt.getPayloadAsUTF8());
        txt = capture.getFrames().poll();
        assertEquals(part2, txt.getPayloadAsUTF8());
    }

    @Test
    public void testShortMaskedText() throws Exception {
        String expectedText = "Hello World";
        byte[] utf = expectedText.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(24);
        buf.put((byte) 0x81);
        buf.put((byte) (0x80 | utf.length));
        MaskedByteBuffer.putMask(buf);
        MaskedByteBuffer.putPayload(buf, utf);
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.TEXT, 1);
        WebSocketFrame txt = capture.getFrames().poll();
        assertEquals(expectedText, txt.getPayloadAsUTF8());
    }

    @Test
    public void testShortMaskedUtf8Text() throws Exception {
        String expectedText = "Hell\uFF4f W\uFF4Frld";

        byte[] utf = expectedText.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(24);
        buf.put((byte) 0x81);
        buf.put((byte) (0x80 | utf.length));
        MaskedByteBuffer.putMask(buf);
        MaskedByteBuffer.putPayload(buf, utf);
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.TEXT, 1);
        WebSocketFrame txt = capture.getFrames().poll();
        assertEquals(expectedText, txt.getPayloadAsUTF8());
    }
}
