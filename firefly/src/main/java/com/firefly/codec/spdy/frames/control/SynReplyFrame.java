package com.firefly.codec.spdy.frames.control;

import com.firefly.codec.spdy.frames.Constants;
import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;
import com.firefly.codec.spdy.frames.Fields;

public class SynReplyFrame extends ControlFrame {

	private final int streamId;
	private final Fields headers;

	public SynReplyFrame(short version, byte flags, int streamId, Fields headers) {
		super(version, ControlFrameType.SYN_REPLY, flags);
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
		return (getFlags() & Constants.FLAG_CLOSE) == Constants.FLAG_CLOSE;
	}

	@Override
	public String toString() {
		return "SynReplyFrame [streamId=" + streamId + ", headers=" + headers
				+ "]";
	}

}
