package com.fireflysource.net.websocket.extension;

import com.fireflysource.common.object.TypeUtils;
import com.fireflysource.net.websocket.decoder.Parser;
import com.fireflysource.net.websocket.decoder.UnitParser;
import com.fireflysource.net.websocket.frame.Frame;
import com.fireflysource.net.websocket.frame.TextFrame;
import com.fireflysource.net.websocket.frame.WebSocketFrame;
import com.fireflysource.net.websocket.model.Extension;
import com.fireflysource.net.websocket.model.ExtensionConfig;
import com.fireflysource.net.websocket.model.IncomingFramesCapture;
import com.fireflysource.net.websocket.model.WebSocketPolicy;
import com.fireflysource.net.websocket.utils.ByteBufferAssert;

import java.nio.ByteBuffer;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

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
            assertNotNull(extClass);

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
            byte[] net;

            for (int i = 0; i < parts; i++) {
                String hex = rawhex[i].replaceAll("\\s*(0x)?", "");
                net = TypeUtils.fromHexString(hex);
                parser.parse(ByteBuffer.wrap(net));
            }
        }

        public void assertHasFrames(String... textFrames) {
            Frame[] frames = new Frame[textFrames.length];
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
                assertEquals(expectedFrames[i].getOpCode(), actual.getOpCode());
                assertEquals(expectedFrames[i].isFin(), actual.isFin());
                assertFalse(actual.isRsv1());
                assertFalse(actual.isRsv2());
                assertFalse(actual.isRsv3());

                ByteBuffer expected = expectedFrames[i].getPayload().slice();
                assertEquals(expected.remaining(), actual.getPayloadLength());
                ByteBufferAssert.assertEquals(prefix + ".payload", expected, actual.getPayload().slice());
            }
        }
    }

    private final WebSocketPolicy policy;
    private final ExtensionFactory factory;

    public ExtensionTool(WebSocketPolicy policy) {
        this.policy = policy;
        this.factory = new WebSocketExtensionFactory();
    }

    public Tester newTester(String parameterizedExtension) {
        return new Tester(parameterizedExtension);
    }
}
