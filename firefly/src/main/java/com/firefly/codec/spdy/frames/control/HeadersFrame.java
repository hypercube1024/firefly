package com.firefly.codec.spdy.frames.control;

import com.firefly.codec.spdy.frames.Constants;
import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;
import com.firefly.codec.spdy.frames.Fields;

public class HeadersFrame extends ControlFrame {
	
	public static final byte FLAG_RESET_COMPRESSION = 2;

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
		return (getFlags() & Constants.FLAG_CLOSE) == Constants.FLAG_CLOSE;
	}

	public boolean isResetCompression() {
		return (getFlags() & FLAG_RESET_COMPRESSION) == FLAG_RESET_COMPRESSION;
	}
}
