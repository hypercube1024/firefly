package com.fireflysource.net.websocket.common.decoder;


import com.fireflysource.net.websocket.common.model.*;
import com.fireflysource.net.websocket.common.utils.MaskedByteBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ClosePayloadParserTest {
    @Test
    public void testGameOver() {
        String expectedReason = "Game Over";

        byte[] utf = expectedReason.getBytes(StandardCharsets.UTF_8);
        ByteBuffer payload = ByteBuffer.allocate(utf.length + 2);
        payload.putChar((char) StatusCode.NORMAL);
        payload.put(utf, 0, utf.length);
        payload.flip();

        ByteBuffer buf = ByteBuffer.allocate(24);
        buf.put((byte) (0x80 | OpCode.CLOSE)); // fin + close
        buf.put((byte) (0x80 | payload.remaining()));
        MaskedByteBuffer.putMask(buf);
        MaskedByteBuffer.putPayload(buf, payload);
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.CLOSE, 1);
        CloseInfo close = new CloseInfo(capture.getFrames().poll());
        assertEquals(StatusCode.NORMAL, close.getStatusCode());
        assertEquals(expectedReason, close.getReason());
    }
}
