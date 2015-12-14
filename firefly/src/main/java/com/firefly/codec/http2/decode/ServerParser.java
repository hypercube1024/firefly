package com.firefly.codec.http2.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ServerParser extends Parser {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final Listener listener;
	private final PrefaceParser prefaceParser;
	private State state = State.PREFACE;
	private boolean notifyPreface = true;

	public ServerParser(Listener listener, int maxDynamicTableSize, int maxHeaderSize) {
		super(listener, maxDynamicTableSize, maxHeaderSize);
		this.listener = listener;
		this.prefaceParser = new PrefaceParser(listener);
	}

	/**
	 * <p>
	 * A direct upgrade is an unofficial upgrade from HTTP/1.1 to HTTP/2.0.
	 * </p>
	 * <p>
	 * A direct upgrade is initiated when
	 * HTTP connection sees a request with these
	 * bytes:
	 * </p>
	 * 
	 * <pre>
	 * PRI * HTTP/2.0\r\n
	 * \r\n
	 * </pre>
	 * <p>
	 * This request is part of the HTTP/2.0 preface, indicating that a HTTP/2.0
	 * client is attempting a h2c direct connection.
	 * </p>
	 * <p>
	 * This is not a standard HTTP/1.1 Upgrade path.
	 * </p>
	 */
	public void directUpgrade() {
		if (state != State.PREFACE)
			throw new IllegalStateException();
		prefaceParser.directUpgrade();
	}

	/**
	 * <p>
	 * The standard HTTP/1.1 upgrade path.
	 * </p>
	 */
	public void standardUpgrade() {
		if (state != State.PREFACE)
			throw new IllegalStateException();
		notifyPreface = false;
	}

	@Override
	public void parse(ByteBuffer buffer) {
		try {
			while (true) {
				switch (state) {
				case PREFACE: {
					if (!prefaceParser.parse(buffer))
						return;
					if (notifyPreface)
						onPreface();
					state = State.SETTINGS;
					break;
				}
				case SETTINGS: {
					if (!parseHeader(buffer))
						return;
					if (getFrameType() != FrameType.SETTINGS.getType() || hasFlag(Flags.ACK)) {
						BufferUtils.clear(buffer);
						notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_preface");
						return;
					}
					if (!parseBody(buffer))
						return;
					state = State.FRAMES;
					break;
				}
				case FRAMES: {
					// Stay forever in the FRAMES state.
					super.parse(buffer);
					return;
				}
				default: {
					throw new IllegalStateException();
				}
				}
			}
		} catch (Throwable x) {
			log.error("server parser error", x);
			BufferUtils.clear(buffer);
			notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "parser_error");
		}
	}

	protected void onPreface() {
		notifyPreface();
	}

	private void notifyPreface() {
		try {
			listener.onPreface();
		} catch (Throwable x) {
			log.error("Failure while notifying listener {}", x, listener);
		}
	}

	public interface Listener extends Parser.Listener {
		public void onPreface();

		public static class Adapter extends Parser.Listener.Adapter implements Listener {
			@Override
			public void onPreface() {
			}
		}
	}

	private enum State {
		PREFACE, SETTINGS, FRAMES
	}
}
