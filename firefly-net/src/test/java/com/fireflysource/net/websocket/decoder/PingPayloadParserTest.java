package com.fireflysource.net.websocket.decoder;


import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.websocket.frame.PingFrame;
import com.fireflysource.net.websocket.model.IncomingFramesCapture;
import com.fireflysource.net.websocket.model.OpCode;
import com.fireflysource.net.websocket.model.WebSocketBehavior;
import com.fireflysource.net.websocket.model.WebSocketPolicy;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PingPayloadParserTest {

    @Test
    public void testBasicPingParsing() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        BufferUtils.clearToFill(buf);
        buf.put(new byte[]
                {(byte) 0x89, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f});
        BufferUtils.flipToFlush(buf, 0);

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.CLIENT);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertHasFrame(OpCode.PING, 1);
        PingFrame ping = (PingFrame) capture.getFrames().poll();

        String actual = BufferUtils.toUTF8String(ping.getPayload());
        assertEquals("Hello", actual);
    }
}
