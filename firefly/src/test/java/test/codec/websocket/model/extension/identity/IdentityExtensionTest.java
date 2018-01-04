package test.codec.websocket.model.extension.identity;

import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.frame.TextFrame;
import com.firefly.codec.websocket.frame.WebSocketFrame;
import com.firefly.codec.websocket.model.Extension;
import com.firefly.codec.websocket.model.OpCode;
import com.firefly.codec.websocket.model.extension.identity.IdentityExtension;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;
import test.codec.websocket.ByteBufferAssert;
import test.codec.websocket.IncomingFramesCapture;
import test.codec.websocket.OutgoingFramesCapture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;

public class IdentityExtensionTest {
    /**
     * Verify that incoming frames are unmodified
     */
    @Test
    public void testIncomingFrames() {
        IncomingFramesCapture capture = new IncomingFramesCapture();

        Extension ext = new IdentityExtension();
        ext.setNextIncomingFrames(capture);

        Frame frame = new TextFrame().setPayload("hello");
        ext.incomingFrame(frame);

        capture.assertFrameCount(1);
        capture.assertHasFrame(OpCode.TEXT, 1);
        WebSocketFrame actual = capture.getFrames().poll();

        Assert.assertThat("Frame.opcode", actual.getOpCode(), is(OpCode.TEXT));
        Assert.assertThat("Frame.fin", actual.isFin(), is(true));
        Assert.assertThat("Frame.rsv1", actual.isRsv1(), is(false));
        Assert.assertThat("Frame.rsv2", actual.isRsv2(), is(false));
        Assert.assertThat("Frame.rsv3", actual.isRsv3(), is(false));

        ByteBuffer expected = BufferUtils.toBuffer("hello", StandardCharsets.UTF_8);
        Assert.assertThat("Frame.payloadLength", actual.getPayloadLength(), is(expected.remaining()));
        ByteBufferAssert.assertEquals("Frame.payload", expected, actual.getPayload().slice());
    }

    /**
     * Verify that outgoing frames are unmodified
     *
     * @throws IOException on test failure
     */
    @Test
    public void testOutgoingFrames() throws IOException {
        OutgoingFramesCapture capture = new OutgoingFramesCapture();

        Extension ext = new IdentityExtension();
        ext.setNextOutgoingFrames(capture);

        Frame frame = new TextFrame().setPayload("hello");
        ext.outgoingFrame(frame, null);

        capture.assertFrameCount(1);
        capture.assertHasFrame(OpCode.TEXT, 1);

        WebSocketFrame actual = capture.getFrames().getFirst();

        Assert.assertThat("Frame.opcode", actual.getOpCode(), is(OpCode.TEXT));
        Assert.assertThat("Frame.fin", actual.isFin(), is(true));
        Assert.assertThat("Frame.rsv1", actual.isRsv1(), is(false));
        Assert.assertThat("Frame.rsv2", actual.isRsv2(), is(false));
        Assert.assertThat("Frame.rsv3", actual.isRsv3(), is(false));

        ByteBuffer expected = BufferUtils.toBuffer("hello", StandardCharsets.UTF_8);
        Assert.assertThat("Frame.payloadLength", actual.getPayloadLength(), is(expected.remaining()));
        ByteBufferAssert.assertEquals("Frame.payload", expected, actual.getPayload().slice());
    }
}
