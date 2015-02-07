package com.firefly.codec.spdy.frames.exception;

import com.firefly.codec.spdy.frames.control.RstStreamFrame.StreamErrorCode;

public class StreamException extends RuntimeException {

	private static final long serialVersionUID = -5314698538391609623L;
	
	private final int streamId;
	private final StreamErrorCode streamStatus;

	public StreamException(int streamId, StreamErrorCode streamStatus) {
		this.streamId = streamId;
		this.streamStatus = streamStatus;
	}

	public StreamException(int streamId, StreamErrorCode streamStatus, String message) {
		super(message);
		this.streamId = streamId;
		this.streamStatus = streamStatus;
	}

	public int getStreamId() {
		return streamId;
	}

	public StreamErrorCode getStreamStatus() {
		return streamStatus;
	}
}
