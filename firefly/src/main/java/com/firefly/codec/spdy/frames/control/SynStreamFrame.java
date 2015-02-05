package com.firefly.codec.spdy.frames.control;

import com.firefly.codec.spdy.frames.Constants;
import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;
import com.firefly.codec.spdy.frames.Fields;

public class SynStreamFrame extends ControlFrame {

	private final int streamId;
	private final int associatedStreamId;
	private final byte priority;
	private final short slot;
	private final Fields headers;

	public SynStreamFrame(short version, byte flags, int streamId,
			int associatedStreamId, byte priority, short slot, Fields headers) {
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

	@Override
	public String toString() {
		return "SynStreamFrame [streamId=" + streamId + ", associatedStreamId="
				+ associatedStreamId + ", priority=" + priority + ", slot="
				+ slot + ", headers=" + headers + "]";
	}
	
	public boolean isClose() {
        return (getFlags() & Constants.FLAG_CLOSE) == Constants.FLAG_CLOSE;
    }

    public boolean isUnidirectional() {
        return (getFlags() & Constants.FLAG_UNIDIRECTIONAL) == Constants.FLAG_UNIDIRECTIONAL;
    }

}
