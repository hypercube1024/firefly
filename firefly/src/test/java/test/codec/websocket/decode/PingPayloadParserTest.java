package test.codec.websocket.decode;

import com.firefly.codec.websocket.decode.Parser;
import com.firefly.codec.websocket.frame.PingFrame;
import com.firefly.codec.websocket.model.OpCode;
import com.firefly.codec.websocket.model.WebSocketBehavior;
import com.firefly.codec.websocket.model.WebSocketPolicy;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;
import test.codec.websocket.IncomingFramesCapture;
import test.codec.websocket.UnitParser;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.is;

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

        capture.assertNoErrors();
        capture.assertHasFrame(OpCode.PING, 1);
        PingFrame ping = (PingFrame) capture.getFrames().poll();

        String actual = BufferUtils.toUTF8String(ping.getPayload());
        Assert.assertThat("PingFrame.payload", actual, is("Hello"));
    }
}
