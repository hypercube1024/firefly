package com.fireflysource.net.http.common.v2.frame;

public class ResetFrame extends Frame {
    public static final int RESET_LENGTH = 4;

    private final int streamId;
    private final int error;

    public ResetFrame(int streamId, int error) {
        super(FrameType.RST_STREAM);
        this.streamId = streamId;
        this.error = error;
    }

    public int getStreamId() {
        return streamId;
    }

    public int getError() {
        return error;
    }

    @Override
    public String toString() {
        return String.format("%s#%d{%s}", super.toString(), streamId, ErrorCode.toString(error, null));
    }
}
