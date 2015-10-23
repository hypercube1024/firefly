package com.firefly.codec.http2.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.model.MetaData;

public class ContinuationBodyParser extends BodyParser {
	private final HeaderBlockParser headerBlockParser;
	private final HeaderBlockFragments headerBlockFragments;
	private State state = State.PREPARE;
	private int length;

	public ContinuationBodyParser(HeaderParser headerParser, Parser.Listener listener,
			HeaderBlockParser headerBlockParser, HeaderBlockFragments headerBlockFragments) {
		super(headerParser, listener);
		this.headerBlockParser = headerBlockParser;
		this.headerBlockFragments = headerBlockFragments;
	}

	@Override
	protected void emptyBody(ByteBuffer buffer) {
		if (hasFlag(Flags.END_HEADERS))
			onHeaders();
	}

	@Override
	public boolean parse(ByteBuffer buffer) {
		while (buffer.hasRemaining()) {
			switch (state) {
			case PREPARE: {
				// SPEC: wrong streamId is treated as connection error.
				if (getStreamId() == 0)
					return connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_continuation_frame");

				if (getStreamId() != headerBlockFragments.getStreamId())
					return connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_continuation_stream");

				length = getBodyLength();
				state = State.FRAGMENT;
				break;
			}
			case FRAGMENT: {
				int remaining = buffer.remaining();
				if (remaining < length) {
					headerBlockFragments.storeFragment(buffer, remaining, false);
					length -= remaining;
				} else {
					boolean last = hasFlag(Flags.END_HEADERS);
					headerBlockFragments.storeFragment(buffer, length, last);
					reset();
					if (last)
						onHeaders();
					return true;
				}
			}
			default: {
				throw new IllegalStateException();
			}
			}
		}
		return false;
	}

	private void onHeaders() {
		ByteBuffer headerBlock = headerBlockFragments.complete();
		MetaData metaData = headerBlockParser.parse(headerBlock, headerBlock.remaining());
		HeadersFrame frame = new HeadersFrame(getStreamId(), metaData, headerBlockFragments.getPriorityFrame(),
				headerBlockFragments.isEndStream());
		notifyHeaders(frame);
	}

	private void reset() {
		state = State.PREPARE;
		length = 0;
	}

	private enum State {
		PREPARE, FRAGMENT
	}
}
