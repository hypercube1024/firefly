package test.codec.websocket.frame;

import com.firefly.codec.websocket.encode.Generator;
import com.firefly.codec.websocket.frame.*;
import com.firefly.codec.websocket.model.CloseInfo;
import com.firefly.codec.websocket.model.StatusCode;
import com.firefly.codec.websocket.model.WebSocketPolicy;
import test.codec.websocket.utils.Hex;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.is;

public class WebSocketFrameTest {

    private Generator strictGenerator;
    private Generator laxGenerator;

    private ByteBuffer generateWholeFrame(Generator generator, Frame frame) {
        ByteBuffer buf = ByteBuffer.allocate(frame.getPayloadLength() + Generator.MAX_HEADER_LENGTH);
        generator.generateWholeFrame(frame, buf);
        BufferUtils.flipToFlush(buf, 0);
        return buf;
    }

    @Before
    public void initGenerator() {
        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();
        strictGenerator = new Generator(policy);
        laxGenerator = new Generator(policy, false);
    }

    private void assertFrameHex(String message, String expectedHex, ByteBuffer actual) {
        String actualHex = Hex.asHex(actual);
        Assert.assertThat("Generated Frame:" + message, actualHex, is(expectedHex));
    }

    @Test
    public void testLaxInvalidClose() {
        WebSocketFrame frame = new CloseFrame().setFin(false);
        ByteBuffer actual = generateWholeFrame(laxGenerator, frame);
        String expected = "0800";
        assertFrameHex("Lax Invalid Close Frame", expected, actual);
    }

    @Test
    public void testLaxInvalidPing() {
        WebSocketFrame frame = new PingFrame().setFin(false);
        ByteBuffer actual = generateWholeFrame(laxGenerator, frame);
        String expected = "0900";
        assertFrameHex("Lax Invalid Ping Frame", expected, actual);
    }

    @Test
    public void testStrictValidClose() {
        CloseInfo close = new CloseInfo(StatusCode.NORMAL);
        ByteBuffer actual = generateWholeFrame(strictGenerator, close.asFrame());
        String expected = "880203E8";
        assertFrameHex("Strict Valid Close Frame", expected, actual);
    }

    @Test
    public void testStrictValidPing() {
        WebSocketFrame frame = new PingFrame();
        ByteBuffer actual = generateWholeFrame(strictGenerator, frame);
        String expected = "8900";
        assertFrameHex("Strict Valid Ping Frame", expected, actual);
    }

    @Test
    public void testRsv1() {
        TextFrame frame = new TextFrame();
        frame.setPayload("Hi");
        frame.setRsv1(true);
        laxGenerator.setRsv1InUse(true);
        ByteBuffer actual = generateWholeFrame(laxGenerator, frame);
        String expected = "C1024869";
        assertFrameHex("Lax Text Frame with RSV1", expected, actual);
    }

    @Test
    public void testRsv2() {
        TextFrame frame = new TextFrame();
        frame.setPayload("Hi");
        frame.setRsv2(true);
        laxGenerator.setRsv2InUse(true);
        ByteBuffer actual = generateWholeFrame(laxGenerator, frame);
        String expected = "A1024869";
        assertFrameHex("Lax Text Frame with RSV2", expected, actual);
    }

    @Test
    public void testRsv3() {
        TextFrame frame = new TextFrame();
        frame.setPayload("Hi");
        frame.setRsv3(true);
        laxGenerator.setRsv3InUse(true);
        ByteBuffer actual = generateWholeFrame(laxGenerator, frame);
        String expected = "91024869";
        assertFrameHex("Lax Text Frame with RSV3", expected, actual);
    }
}
