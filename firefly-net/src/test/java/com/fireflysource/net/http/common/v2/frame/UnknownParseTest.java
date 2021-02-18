package com.fireflysource.net.http.common.v2.frame;

import com.fireflysource.net.http.common.v2.decoder.Parser;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UnknownParseTest {

    @Test
    void testParse() {
        testParse(Function.identity());
    }

    @Test
    void testParseOneByteAtATime() {
        testParse(buffer -> ByteBuffer.wrap(new byte[]{buffer.get()}));
    }

    @Test
    void testInvalidFrameSize() {
        AtomicInteger failure = new AtomicInteger();
        Parser parser = new Parser(new Parser.Listener.Adapter(), 4096, 8192);
        parser.init(listener -> new Parser.Listener.Wrapper(listener) {
            @Override
            public void onConnectionFailure(int error, String reason) {
                failure.set(error);
            }
        });
        parser.setMaxFrameLength(Frame.DEFAULT_MAX_LENGTH);

        // 0x4001 == 16385 which is > Frame.DEFAULT_MAX_LENGTH.
        byte[] bytes = new byte[]{0, 0x40, 0x01, 64, 0, 0, 0, 0, 0};
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining())
            parser.parse(buffer);

        assertEquals(ErrorCode.FRAME_SIZE_ERROR.code, failure.get());
    }

    private void testParse(Function<ByteBuffer, ByteBuffer> fn) {
        AtomicBoolean failure = new AtomicBoolean();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onConnectionFailure(int error, String reason) {
                failure.set(true);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        // Iterate a few times to be sure the parser is properly reset.
        for (int i = 0; i < 2; ++i) {
            byte[] bytes = new byte[]{0, 0, 4, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining())
                parser.parse(fn.apply(buffer));
        }

        assertFalse(failure.get());
    }
}
