package com.firefly.codec.http2.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.PrefaceFrame;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class PrefaceParser {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final Parser.Listener listener;
	private int cursor;

	public PrefaceParser(Parser.Listener listener) {
		this.listener = listener;
	}

	/**
	 * <p>
	 * Advances this parser after the
	 * {@link PrefaceFrame#PREFACE_PREAMBLE_BYTES}.
	 * </p>
	 * <p>
	 * This allows the HTTP/1.1 parser to parse the preamble of the preface,
	 * which is a legal HTTP/1.1 request, and this parser will parse the
	 * remaining bytes, that are not parseable by a HTTP/1.1 parser.
	 * </p>
	 */
	protected void directUpgrade() {
		if (cursor != 0)
			throw new IllegalStateException();
		cursor = PrefaceFrame.PREFACE_PREAMBLE_BYTES.length;
	}

	public boolean parse(ByteBuffer buffer) {
		while (buffer.hasRemaining()) {
			int currByte = buffer.get();
			if (currByte != PrefaceFrame.PREFACE_BYTES[cursor]) {
				BufferUtils.clear(buffer);
				notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_preface");
				return false;
			}
			++cursor;
			if (cursor == PrefaceFrame.PREFACE_BYTES.length) {
				cursor = 0;
				if (log.isDebugEnabled())
					log.debug("Parsed preface bytes");
				return true;
			}
		}
		return false;
	}

	protected void notifyConnectionFailure(int error, String reason) {
		try {
			listener.onConnectionFailure(error, reason);
		} catch (Throwable x) {
			log.error("Failure while notifying listener {}", x, listener);
		}
	}
}
