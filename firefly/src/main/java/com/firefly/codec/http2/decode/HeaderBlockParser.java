package com.firefly.codec.http2.decode;

import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.hpack.HpackDecoder;
import com.firefly.codec.http2.hpack.HpackException;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class HeaderBlockParser {
	private static final Logger LOG = LoggerFactory.getLogger("firefly-system");

	public static final MetaData STREAM_FAILURE = new MetaData(HttpVersion.HTTP_2, null);
	public static final MetaData SESSION_FAILURE = new MetaData(HttpVersion.HTTP_2, null);

	private final HeaderParser headerParser;
	private final HpackDecoder hpackDecoder;
	private final BodyParser notifier;
	private ByteBuffer blockBuffer;

	public HeaderBlockParser(HeaderParser headerParser, HpackDecoder hpackDecoder, BodyParser notifier) {
		this.headerParser = headerParser;
		this.hpackDecoder = hpackDecoder;
		this.notifier = notifier;
	}

	/**
	 * Parses @{code blockLength} HPACK bytes from the given {@code buffer}.
	 *
	 * @param buffer      the buffer to parse
	 * @param blockLength the length of the HPACK block
	 * @return null, if the buffer contains less than {@code blockLength} bytes;
	 * {@link #STREAM_FAILURE} if parsing the HPACK block produced a stream failure;
	 * {@link #SESSION_FAILURE} if parsing the HPACK block produced a session failure;
	 * a valid MetaData object if the parsing was successful.
	 */
	public MetaData parse(ByteBuffer buffer, int blockLength) {
		// We must wait for the all the bytes of the header block to arrive.
		// If they are not all available, accumulate them.
		// When all are available, decode them.

		int accumulated = blockBuffer == null ? 0 : blockBuffer.position();
		int remaining = blockLength - accumulated;

		if (buffer.remaining() < remaining) {
			if (blockBuffer == null) {
				blockBuffer = ByteBuffer.allocate(blockLength);
				BufferUtils.clearToFill(blockBuffer);
			}
			blockBuffer.put(buffer);
			return null;
		} else {
			int limit = buffer.limit();
			buffer.limit(buffer.position() + remaining);
			ByteBuffer toDecode;
			if (blockBuffer != null) {
				blockBuffer.put(buffer);
				BufferUtils.flipToFlush(blockBuffer, 0);
				toDecode = blockBuffer;
			} else {
				toDecode = buffer;
			}

			try {
				return hpackDecoder.decode(toDecode);
			} catch (HpackException.StreamException x) {
				if (LOG.isDebugEnabled())
					LOG.debug("hpack exception", x);
				notifier.streamFailure(headerParser.getStreamId(), ErrorCode.PROTOCOL_ERROR.code, "invalid_hpack_block");
				return STREAM_FAILURE;
			} catch (HpackException.CompressionException x) {
				if (LOG.isDebugEnabled())
					LOG.debug("hpack exception", x);
				notifier.connectionFailure(buffer, ErrorCode.COMPRESSION_ERROR.code, "invalid_hpack_block");
				return SESSION_FAILURE;
			} catch (HpackException.SessionException x) {
				if (LOG.isDebugEnabled())
					LOG.debug("hpack exception", x);
				notifier.connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_hpack_block");
				return SESSION_FAILURE;
			} finally {
				buffer.limit(limit);

				if (blockBuffer != null) {
					blockBuffer = null;
				}
			}
		}
	}
}
