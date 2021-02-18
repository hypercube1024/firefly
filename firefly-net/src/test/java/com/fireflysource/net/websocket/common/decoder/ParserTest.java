package com.fireflysource.net.websocket.common.decoder;

import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.websocket.common.encoder.UnitGenerator;
import com.fireflysource.net.websocket.common.exception.ProtocolException;
import com.fireflysource.net.websocket.common.frame.*;
import com.fireflysource.net.websocket.common.model.*;
import com.fireflysource.net.websocket.common.utils.Hex;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {
    /**
     * Similar to the server side 5.15 testcase. A normal 2 fragment text text message, followed by another continuation.
     */
    @Test
    public void testParseCase515() {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new TextFrame().setPayload("fragment1").setFin(false));
        send.add(new ContinuationFrame().setPayload("fragment2").setFin(true));
        send.add(new ContinuationFrame().setPayload("fragment3").setFin(false)); // bad frame
        send.add(new TextFrame().setPayload("fragment4").setFin(true));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        ProtocolException x = assertThrows(ProtocolException.class, () -> parser.parseQuietly(completeBuf));
        assertTrue(x.getMessage().contains("CONTINUATION frame without prior !FIN"));
    }

    /**
     * Similar to the server side 5.18 testcase. Text message fragmented as 2 frames, both as opcode=TEXT
     */
    @Test
    public void testParseCase518() {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new TextFrame().setPayload("fragment1").setFin(false));
        send.add(new TextFrame().setPayload("fragment2").setFin(true)); // bad frame, must be continuation
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        ProtocolException x = assertThrows(ProtocolException.class, () -> parser.parseQuietly(completeBuf));
        assertTrue(x.getMessage().contains("Unexpected TEXT frame"));
    }

    /**
     * Similar to the server side 5.19 testcase. text message, send in 5 frames/fragments, with 2 pings in the mix.
     */
    @Test
    public void testParseCase519() {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new TextFrame().setPayload("f1").setFin(false));
        send.add(new ContinuationFrame().setPayload(",f2").setFin(false));
        send.add(new PingFrame().setPayload("pong-1"));
        send.add(new ContinuationFrame().setPayload(",f3").setFin(false));
        send.add(new ContinuationFrame().setPayload(",f4").setFin(false));
        send.add(new PingFrame().setPayload("pong-2"));
        send.add(new ContinuationFrame().setPayload(",f5").setFin(true));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parseQuietly(completeBuf);

        capture.assertHasFrame(OpCode.TEXT, 1);
        capture.assertHasFrame(OpCode.CONTINUATION, 4);
        capture.assertHasFrame(OpCode.CLOSE, 1);
        capture.assertHasFrame(OpCode.PING, 2);
    }

    /**
     * Similar to the server side 5.6 testcase. pong, then text, then close frames.
     */
    @Test
    public void testParseCase56() {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new PongFrame().setPayload("ping"));
        send.add(new TextFrame().setPayload("hello, world"));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(completeBuf);

        capture.assertHasFrame(OpCode.TEXT, 1);
        capture.assertHasFrame(OpCode.CLOSE, 1);
        capture.assertHasFrame(OpCode.PONG, 1);
    }

    /**
     * Similar to the server side 6.2.3 testcase. Lots of small 1 byte UTF8 Text frames, representing 1 overall text message.
     */
    @Test
    public void testParseCase623() {
        String utf8 = "Hello-\uC2B5@\uC39F\uC3A4\uC3BC\uC3A0\uC3A1-UTF-8!!";
        byte[] msg = StringUtils.getUtf8Bytes(utf8);

        List<WebSocketFrame> send = new ArrayList<>();
        int textCount = 0;
        int continuationCount = 0;
        int len = msg.length;
        boolean continuation = false;
        byte[] mini;
        for (int i = 0; i < len; i++) {
            DataFrame frame = null;
            if (continuation) {
                frame = new ContinuationFrame();
                continuationCount++;
            } else {
                frame = new TextFrame();
                textCount++;
            }
            mini = new byte[1];
            mini[0] = msg[i];
            frame.setPayload(ByteBuffer.wrap(mini));
            boolean isLast = (i >= (len - 1));
            frame.setFin(isLast);
            send.add(frame);
            continuation = true;
        }
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(completeBuf);

        capture.assertHasFrame(OpCode.TEXT, textCount);
        capture.assertHasFrame(OpCode.CONTINUATION, continuationCount);
        capture.assertHasFrame(OpCode.CLOSE, 1);
    }

    @Test
    public void testParseNothing() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        // Put nothing in the buffer.
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        assertEquals(0, capture.getFrames().size());
    }

    @Test
    public void testWindowedParseLargeFrame() {
        // Create frames
        byte[] payload = new byte[65536];
        Arrays.fill(payload, (byte) '*');

        List<WebSocketFrame> frames = new ArrayList<>();
        TextFrame text = new TextFrame();
        text.setPayload(ByteBuffer.wrap(payload));
        text.setMask(Hex.asByteArray("11223344"));
        frames.add(text);
        frames.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        // Build up raw (network bytes) buffer
        ByteBuffer networkBytes = UnitGenerator.generate(frames);

        // Parse, in 4096 sized windows
        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        while (networkBytes.remaining() > 0) {
            ByteBuffer window = networkBytes.slice();
            int windowSize = Math.min(window.remaining(), 4096);
            window.limit(windowSize);
            parser.parse(window);
            networkBytes.position(networkBytes.position() + windowSize);
        }

        assertEquals(2, capture.getFrames().size());
        WebSocketFrame frame = capture.getFrames().poll();
        assertEquals(OpCode.TEXT, frame.getOpCode());
        ByteBuffer actualPayload = frame.getPayload();
        assertEquals(payload.length, actualPayload.remaining());
        // Should be all '*' characters (if masking is correct)
        for (int i = actualPayload.position(); i < actualPayload.remaining(); i++) {
            assertEquals((byte) '*', actualPayload.get(i));
        }
    }
}
