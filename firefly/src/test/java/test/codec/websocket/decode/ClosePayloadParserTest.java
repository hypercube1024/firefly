package test.codec.websocket.decode;

import com.firefly.codec.websocket.decode.Parser;
import com.firefly.codec.websocket.model.CloseInfo;
import com.firefly.codec.websocket.model.OpCode;
import com.firefly.codec.websocket.model.StatusCode;
import com.firefly.codec.websocket.model.WebSocketBehavior;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.codec.websocket.utils.MaskedByteBuffer;
import org.junit.Assert;
import org.junit.Test;
import test.codec.websocket.IncomingFramesCapture;
import test.codec.websocket.UnitParser;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;

public class ClosePayloadParserTest {
    @Test
    public void testGameOver() {
        String expectedReason = "Game Over";

        byte utf[] = expectedReason.getBytes(StandardCharsets.UTF_8);
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

        capture.assertNoErrors();
        capture.assertHasFrame(OpCode.CLOSE, 1);
        CloseInfo close = new CloseInfo(capture.getFrames().poll());
        Assert.assertThat("CloseFrame.statusCode", close.getStatusCode(), is(StatusCode.NORMAL));
        Assert.assertThat("CloseFrame.data", close.getReason(), is(expectedReason));
    }
}
