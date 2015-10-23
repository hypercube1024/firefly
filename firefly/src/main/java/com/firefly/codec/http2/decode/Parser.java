package com.firefly.codec.http2.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.frame.GoAwayFrame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PingFrame;
import com.firefly.codec.http2.frame.PriorityFrame;
import com.firefly.codec.http2.frame.PushPromiseFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.codec.http2.hpack.HpackDecoder;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * <p>
 * The HTTP/2 protocol parser.
 * </p>
 * <p>
 * This parser makes use of the {@link HeaderParser} and of {@link BodyParser}s
 * to parse HTTP/2 frames.
 * </p>
 */
public class Parser {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final Listener listener;
	private final HeaderParser headerParser;
	private final BodyParser[] bodyParsers;
	private boolean continuation;
	private State state = State.HEADER;

	public Parser(Listener listener, int maxDynamicTableSize, int maxHeaderSize) {
		this.listener = listener;
		this.headerParser = new HeaderParser();
		this.bodyParsers = new BodyParser[FrameType.values().length];
		
		HeaderBlockParser headerBlockParser = new HeaderBlockParser(new HpackDecoder(maxDynamicTableSize, maxHeaderSize));
        HeaderBlockFragments headerBlockFragments = new HeaderBlockFragments();

		bodyParsers[FrameType.DATA.getType()] = new DataBodyParser(headerParser, listener);
	}

	private void reset() {
		headerParser.reset();
		state = State.HEADER;
	}

	/**
	 * <p>
	 * Parses the given {@code buffer} bytes and emit events to a
	 * {@link Listener}.
	 * </p>
	 * <p>
	 * When this method returns, the buffer may not be fully consumed, so
	 * invocations to this method should be wrapped in a loop:
	 * </p>
	 * 
	 * <pre>
	 * while (buffer.hasRemaining())
	 * 	parser.parse(buffer);
	 * </pre>
	 *
	 * @param buffer
	 *            the buffer to parse
	 */
	public void parse(ByteBuffer buffer) {
		try {
			while (true) {
				switch (state) {
				case HEADER: {
					if (!parseHeader(buffer))
						return;
					break;
				}
				case BODY: {
					if (!parseBody(buffer))
						return;
					break;
				}
				default: {
					throw new IllegalStateException();
				}
				}
			}
		} catch (Throwable x) {
			log.error("HTTP2 parsing error", x);
			BufferUtils.clear(buffer);
			notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "parser_error");
		}
	}

	protected boolean parseHeader(ByteBuffer buffer) {
		if (!headerParser.parse(buffer))
			return false;

		int frameType = getFrameType();
		if (log.isDebugEnable())
			log.debug("Parsed {} frame header", FrameType.from(frameType));

		if (continuation) {
			if (frameType != FrameType.CONTINUATION.getType()) {
				// SPEC: CONTINUATION frames must be consecutive.
				BufferUtils.clear(buffer);
				notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "continuation_frame_expected");
				return false;
			}
			if (headerParser.hasFlag(Flags.END_HEADERS)) {
				continuation = false;
			}
		} else {
			if (frameType == FrameType.HEADERS.getType() && !headerParser.hasFlag(Flags.END_HEADERS)) {
				continuation = true;
			}
		}
		state = State.BODY;
		return true;
	}

	protected boolean parseBody(ByteBuffer buffer) {
		int type = getFrameType();
		if (type < 0 || type >= bodyParsers.length) {
			BufferUtils.clear(buffer);
			notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "unknown_frame_type_" + type);
			return false;
		}

		FrameType frameType = FrameType.from(type);
		if (log.isDebugEnable())
			log.debug("Parsing {} frame", frameType);

		BodyParser bodyParser = bodyParsers[type];
		if (headerParser.getLength() == 0) {
			bodyParser.emptyBody(buffer);
		} else {
			if (!bodyParser.parse(buffer))
				return false;
		}
		if (log.isDebugEnable())
			log.debug("Parsed {} frame", frameType);
		reset();
		return true;
	}

	protected int getFrameType() {
		return headerParser.getFrameType();
	}

	protected boolean hasFlag(int bit) {
		return headerParser.hasFlag(bit);
	}

	protected void notifyConnectionFailure(int error, String reason) {
		try {
			listener.onConnectionFailure(error, reason);
		} catch (Throwable x) {
			log.error("Failure while notifying listener {}", x, listener);
		}
	}

	public interface Listener {
		public void onData(DataFrame frame);

		public void onHeaders(HeadersFrame frame);

		public void onPriority(PriorityFrame frame);

		public void onReset(ResetFrame frame);

		public void onSettings(SettingsFrame frame);

		public void onPushPromise(PushPromiseFrame frame);

		public void onPing(PingFrame frame);

		public void onGoAway(GoAwayFrame frame);

		public void onWindowUpdate(WindowUpdateFrame frame);

		public void onConnectionFailure(int error, String reason);

		public static class Adapter implements Listener {
			@Override
			public void onData(DataFrame frame) {
			}

			@Override
			public void onHeaders(HeadersFrame frame) {
			}

			@Override
			public void onPriority(PriorityFrame frame) {
			}

			@Override
			public void onReset(ResetFrame frame) {
			}

			@Override
			public void onSettings(SettingsFrame frame) {
			}

			@Override
			public void onPushPromise(PushPromiseFrame frame) {
			}

			@Override
			public void onPing(PingFrame frame) {
			}

			@Override
			public void onGoAway(GoAwayFrame frame) {
			}

			@Override
			public void onWindowUpdate(WindowUpdateFrame frame) {
			}

			@Override
			public void onConnectionFailure(int error, String reason) {
				log.warn("Connection failure: {}/{}", error, reason);
			}
		}
	}

	private enum State {
		HEADER, BODY
	}
}
