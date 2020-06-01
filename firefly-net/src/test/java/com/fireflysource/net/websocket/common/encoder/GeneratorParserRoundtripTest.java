package com.fireflysource.net.websocket.common.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.websocket.common.decoder.Parser;
import com.fireflysource.net.websocket.common.frame.TextFrame;
import com.fireflysource.net.websocket.common.frame.WebSocketFrame;
import com.fireflysource.net.websocket.common.model.IncomingFramesCapture;
import com.fireflysource.net.websocket.common.model.OpCode;
import com.fireflysource.net.websocket.common.model.WebSocketPolicy;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratorParserRoundtripTest {

    @Test
    public void testParserAndGenerator() {
        WebSocketPolicy policy = WebSocketPolicy.newClientPolicy();
        Generator gen = new Generator(policy);
        Parser parser = new Parser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        String message = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

        ByteBuffer out = BufferUtils.allocate(8192);
        // Generate Buffer
        BufferUtils.flipToFill(out);
        WebSocketFrame frame = new TextFrame().setPayload(message);
        ByteBuffer header = gen.generateHeaderBytes(frame);
        ByteBuffer payload = frame.getPayload();
        out.put(header);
        out.put(payload);

        // Parse Buffer
        BufferUtils.flipToFlush(out, 0);
        parser.parse(out);

        // Validate
        capture.assertHasFrame(OpCode.TEXT, 1);

        TextFrame txt = (TextFrame) capture.getFrames().poll();
        assertEquals(message, txt.getPayloadAsUTF8());
    }

    @Test
    public void testParserAndGeneratorMasked() {
        Generator gen = new Generator(WebSocketPolicy.newClientPolicy());
        Parser parser = new Parser(WebSocketPolicy.newServerPolicy());
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        String message = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

        ByteBuffer out = BufferUtils.allocate(8192);
        BufferUtils.flipToFill(out);
        // Setup Frame
        WebSocketFrame frame = new TextFrame().setPayload(message);

        // Add masking
        byte[] mask = new byte[4];
        Arrays.fill(mask, (byte) 0xFF);
        frame.setMask(mask);

        // Generate Buffer
        ByteBuffer header = gen.generateHeaderBytes(frame);
        ByteBuffer payload = frame.getPayload();
        out.put(header);
        out.put(payload);

        // Parse Buffer
        BufferUtils.flipToFlush(out, 0);
        parser.parse(out);

        // Validate
        capture.assertHasFrame(OpCode.TEXT, 1);

        TextFrame txt = (TextFrame) capture.getFrames().poll();
        assertTrue(txt.isMasked());
        assertEquals(message, txt.getPayloadAsUTF8());
    }
}
