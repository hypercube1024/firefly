package test.codec.websocket.model.extension;

import com.firefly.codec.websocket.decode.Parser;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.frame.TextFrame;
import com.firefly.codec.websocket.frame.WebSocketFrame;
import com.firefly.codec.websocket.model.*;
import com.firefly.codec.websocket.model.extension.AbstractExtension;
import com.firefly.codec.websocket.model.extension.ExtensionFactory;
import com.firefly.codec.websocket.model.extension.WebSocketExtensionFactory;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.utils.lang.TypeUtils;
import org.junit.Assert;
import test.codec.websocket.ByteBufferAssert;
import test.codec.websocket.IncomingFramesCapture;
import test.codec.websocket.UnitParser;

import java.nio.ByteBuffer;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ExtensionTool {
    public class Tester {
        private String requestedExtParams;
        private ExtensionConfig extConfig;
        private Extension ext;
        private Parser parser;
        private IncomingFramesCapture capture;

        private Tester(String parameterizedExtension) {
            this.requestedExtParams = parameterizedExtension;
            this.extConfig = ExtensionConfig.parse(parameterizedExtension);
            Class<?> extClass = factory.getExtension(extConfig.getName());
            Assert.assertThat("extClass", extClass, notNullValue());

            this.parser = new UnitParser(policy);
        }

        public String getRequestedExtParams() {
            return requestedExtParams;
        }

        public void assertNegotiated(String expectedNegotiation) {
            this.ext = factory.newInstance(extConfig);
            if (ext instanceof AbstractExtension) {
                ((AbstractExtension) ext).setPolicy(policy);
            }

            this.capture = new IncomingFramesCapture();
            this.ext.setNextIncomingFrames(capture);

            this.parser.configureFromExtensions(Collections.singletonList(ext));
            this.parser.setIncomingFramesHandler(ext);
        }

        public void parseIncomingHex(String... rawhex) {
            int parts = rawhex.length;
            byte net[];

            for (int i = 0; i < parts; i++) {
                String hex = rawhex[i].replaceAll("\\s*(0x)?", "");
                net = TypeUtils.fromHexString(hex);
                parser.parse(ByteBuffer.wrap(net));
            }
        }

        public void assertHasFrames(String... textFrames) {
            Frame frames[] = new Frame[textFrames.length];
            for (int i = 0; i < frames.length; i++) {
                frames[i] = new TextFrame().setPayload(textFrames[i]);
            }
            assertHasFrames(frames);
        }

        public void assertHasFrames(Frame... expectedFrames) {
            int expectedCount = expectedFrames.length;
            capture.assertFrameCount(expectedCount);

            for (int i = 0; i < expectedCount; i++) {
                WebSocketFrame actual = capture.getFrames().poll();

                String prefix = String.format("frame[%d]", i);
                Assert.assertThat(prefix + ".opcode", actual.getOpCode(), is(expectedFrames[i].getOpCode()));
                Assert.assertThat(prefix + ".fin", actual.isFin(), is(expectedFrames[i].isFin()));
                Assert.assertThat(prefix + ".rsv1", actual.isRsv1(), is(false));
                Assert.assertThat(prefix + ".rsv2", actual.isRsv2(), is(false));
                Assert.assertThat(prefix + ".rsv3", actual.isRsv3(), is(false));

                ByteBuffer expected = expectedFrames[i].getPayload().slice();
                Assert.assertThat(prefix + ".payloadLength", actual.getPayloadLength(), is(expected.remaining()));
                ByteBufferAssert.assertEquals(prefix + ".payload", expected, actual.getPayload().slice());
            }
        }
    }

    private final WebSocketPolicy policy;
    private final ExtensionFactory factory;

    public ExtensionTool(WebSocketPolicy policy) {
        this.policy = policy;
        factory = new WebSocketExtensionFactory();
    }

    public Tester newTester(String parameterizedExtension) {
        return new Tester(parameterizedExtension);
    }
}
