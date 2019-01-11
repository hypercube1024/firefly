package com.fireflysource.net.http.common.v2.decoder;

import com.fireflysource.net.http.common.v2.frame.ErrorCode;
import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame;

import java.nio.ByteBuffer;

public class WindowUpdateBodyParser extends BodyParser {
    private State state = State.PREPARE;
    private int cursor;
    private int windowDelta;

    public WindowUpdateBodyParser(HeaderParser headerParser, Parser.Listener listener) {
        super(headerParser, listener);
    }

    private void reset() {
        state = State.PREPARE;
        cursor = 0;
        windowDelta = 0;
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            switch (state) {
                case PREPARE: {
                    int length = getBodyLength();
                    if (length != 4)
                        return connectionFailure(buffer, ErrorCode.FRAME_SIZE_ERROR.code, "invalid_window_update_frame");
                    state = State.WINDOW_DELTA;
                    break;
                }
                case WINDOW_DELTA: {
                    if (buffer.remaining() >= 4) {
                        windowDelta = buffer.getInt() & 0x7F_FF_FF_FF;
                        return onWindowUpdate(windowDelta);
                    } else {
                        state = State.WINDOW_DELTA_BYTES;
                        cursor = 4;
                    }
                    break;
                }
                case WINDOW_DELTA_BYTES: {
                    byte currByte = buffer.get();
                    --cursor;
                    windowDelta += (currByte & 0xFF) << 8 * cursor;
                    if (cursor == 0) {
                        windowDelta &= 0x7F_FF_FF_FF;
                        return onWindowUpdate(windowDelta);
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }
        return false;
    }

    private boolean onWindowUpdate(int windowDelta) {
        WindowUpdateFrame frame = new WindowUpdateFrame(getStreamId(), windowDelta);
        reset();
        notifyWindowUpdate(frame);
        return true;
    }

    private enum State {
        PREPARE, WINDOW_DELTA, WINDOW_DELTA_BYTES
    }
}
