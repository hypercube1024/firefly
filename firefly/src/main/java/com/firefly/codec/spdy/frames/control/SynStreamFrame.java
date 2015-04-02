package com.firefly.codec.spdy.frames.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.frames.Constants;
import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;
import com.firefly.codec.spdy.frames.control.RstStreamFrame.StreamErrorCode;
import com.firefly.codec.spdy.frames.exception.SessionException;
import com.firefly.codec.spdy.frames.exception.StreamException;

public class SynStreamFrame extends ControlFrame {
	
	public static final byte FLAG_UNIDIRECTIONAL = 2;
	public static final byte FLAG_FIN = 1;

	private final int streamId;
	private final int associatedStreamId;
	private final byte priority;
	private final byte slot;
	private final Fields headers;

	public SynStreamFrame(short version, byte flags, int streamId,
			int associatedStreamId, byte priority, byte slot, Fields headers) {
		super(version, ControlFrameType.SYN_STREAM, flags);
		this.streamId = streamId;
		this.associatedStreamId = associatedStreamId;
		this.priority = priority;
		this.slot = slot;
		this.headers = headers;
	}

	public int getStreamId() {
		return streamId;
	}

	public int getAssociatedStreamId() {
		return associatedStreamId;
	}

	public byte getPriority() {
		return priority;
	}

	public short getSlot() {
		return slot;
	}

	public Fields getHeaders() {
		return headers;
	}

	public boolean isClose() {
		return (getFlags() & Constants.FLAG_CLOSE) == Constants.FLAG_CLOSE;
	}

	public boolean isUnidirectional() {
		return (getFlags() & FLAG_UNIDIRECTIONAL) == FLAG_UNIDIRECTIONAL;
	}

	@Override
	public String toString() {
		return "SynStreamFrame [streamId=" + streamId + ", associatedStreamId="
				+ associatedStreamId + ", priority=" + priority + ", slot="
				+ slot + ", headers=" + headers + ", toString()="
				+ super.toString() + "]";
	}

	@Override
	public ByteBuffer toByteBuffer() {
		ByteBuffer headersBuffer = headers.toByteBuffer();
        int frameLength = 10 + headersBuffer.remaining();
        if (frameLength > 0xFF_FF_FF) {
            // Too many headers, but unfortunately we have already modified the compression
            // context, so we have no other choice than tear down the connection.
            throw new SessionException(SessionStatus.PROTOCOL_ERROR, "Too many headers");
        }
        int totalLength = ControlFrame.HEADER_LENGTH + frameLength;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        generateControlFrameHeader(frameLength, buffer);
        buffer.putInt(streamId & 0x7F_FF_FF_FF);
        buffer.putInt(associatedStreamId & 0x7F_FF_FF_FF);
        writePriority(streamId, getVersion(), priority, buffer);
        buffer.put(slot);
        buffer.put(headersBuffer);
        buffer.flip();
		return buffer;
	}
	
	private void writePriority(int streamId, short version, byte priority, ByteBuffer buffer) {
        switch (version) {
            case VERSION:
                priority <<= 5;
                break;
            default:
                throw new StreamException(streamId, StreamErrorCode.UNSUPPORTED_VERSION);
        }
        buffer.put(priority);
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + associatedStreamId;
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		result = prime * result + priority;
		result = prime * result + slot;
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
		SynStreamFrame other = (SynStreamFrame) obj;
		if (associatedStreamId != other.associatedStreamId)
			return false;
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
			return false;
		if (priority != other.priority)
			return false;
		if (slot != other.slot)
			return false;
		if (streamId != other.streamId)
			return false;
		return true;
	}
	
}
