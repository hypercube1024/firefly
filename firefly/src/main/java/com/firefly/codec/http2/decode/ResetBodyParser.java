package com.firefly.codec.http2.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.ResetFrame;

public class ResetBodyParser extends BodyParser {
	private State state = State.PREPARE;
	private int cursor;
	private int error;

	public ResetBodyParser(HeaderParser headerParser, Parser.Listener listener) {
		super(headerParser, listener);
	}

	private void reset() {
		state = State.PREPARE;
		cursor = 0;
		error = 0;
	}

	@Override
	public boolean parse(ByteBuffer buffer) {
		while (buffer.hasRemaining()) {
			switch (state) {
			case PREPARE: {
				// SPEC: wrong streamId is treated as connection error.
				if (getStreamId() == 0)
					return connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_rst_stream_frame");
				int length = getBodyLength();
				if (length != 4)
					return connectionFailure(buffer, ErrorCode.FRAME_SIZE_ERROR.code, "invalid_rst_stream_frame");
				state = State.ERROR;
				break;
			}
			case ERROR: {
				if (buffer.remaining() >= 4) {
					return onReset(buffer.getInt());
				} else {
					state = State.ERROR_BYTES;
					cursor = 4;
				}
				break;
			}
			case ERROR_BYTES: {
				int currByte = buffer.get() & 0xFF;
				--cursor;
				error += currByte << (8 * cursor);
				if (cursor == 0)
					return onReset(error);
				break;
			}
			default: {
				throw new IllegalStateException();
			}
			}
		}
		return false;
	}

	private boolean onReset(int error) {
		ResetFrame frame = new ResetFrame(getStreamId(), error);
		reset();
		notifyReset(frame);
		return true;
	}

	private enum State {
		PREPARE, ERROR, ERROR_BYTES
	}
}
