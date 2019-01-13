package com.fireflysource.net.http.common.v2.frame;

import com.fireflysource.net.http.common.v2.decoder.Parser;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaxFrameSizeParseTest {

    @Test
    void testMaxFrameSize() {
        int maxFrameLength = Frame.DEFAULT_MAX_LENGTH + 16;

        AtomicInteger failure = new AtomicInteger();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onConnectionFailure(int error, String reason) {
                failure.set(error);
            }
        }, 4096, 8192);
        parser.setMaxFrameLength(maxFrameLength);
        parser.init(UnaryOperator.identity());

        // Iterate a few times to be sure the parser is properly reset.
        for (int i = 0; i < 2; ++i) {
            byte[] bytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0};
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.putInt(0, maxFrameLength + 1);
            buffer.position(1);
            while (buffer.hasRemaining())
                parser.parse(buffer);
        }

        assertEquals(ErrorCode.FRAME_SIZE_ERROR.code, failure.get());
    }
}
