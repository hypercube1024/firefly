package com.firefly.codec.spdy.frames.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;
import com.firefly.codec.spdy.frames.exception.SessionException;

public class HeadersFrame extends ControlFrame {
	
	public static final byte FLAG_RESET_COMPRESSION = 2;
	public static final byte FLAG_FIN = 1;

	private final int streamId;
	private final Fields headers;

	public HeadersFrame(short version, byte flags, int streamId, Fields headers) {
		super(version, ControlFrameType.HEADERS, flags);
		this.streamId = streamId;
		this.headers = headers;
	}

	public int getStreamId() {
		return streamId;
	}

	public Fields getHeaders() {
		return headers;
	}

	public boolean isClose() {
		return (getFlags() & FLAG_FIN) == FLAG_FIN;
	}

	public boolean isResetCompression() {
		return (getFlags() & FLAG_RESET_COMPRESSION) == FLAG_RESET_COMPRESSION;
	}

	@Override
	public ByteBuffer toByteBuffer() {
		ByteBuffer headersBuffer = headers.toByteBuffer();
		int frameLength = 4 + headersBuffer.remaining();
		if (frameLength > 0xFF_FF_FF) {
            // Too many headers, but unfortunately we have already modified the compression
            // context, so we have no other choice than tear down the connection.
            throw new SessionException(SessionStatus.PROTOCOL_ERROR, "Too many headers");
        }
		int totalLength = ControlFrame.HEADER_LENGTH + frameLength;
		
		ByteBuffer buffer = ByteBuffer.allocate(totalLength);
		generateControlFrameHeader(frameLength, buffer);
        buffer.putInt(streamId & 0x7F_FF_FF_FF);
        buffer.put(headersBuffer);
        buffer.flip();
        return buffer;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		result = prime * result + streamId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		HeadersFrame other = (HeadersFrame) obj;
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
			return false;
		if (streamId != other.streamId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "HeadersFrame [streamId=" + streamId + ", headers=" + headers
				+ ", toString()=" + super.toString() + "]";
	}
	
}
