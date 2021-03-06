package com.fireflysource.net.http.common.v2.frame;

public class WindowUpdateFrame extends Frame {
    public static final int WINDOW_UPDATE_LENGTH = 4;

    private final int streamId;
    private final int windowDelta;

    public WindowUpdateFrame(int streamId, int windowDelta) {
        super(FrameType.WINDOW_UPDATE);
        this.streamId = streamId;
        this.windowDelta = windowDelta;
    }

    public int getStreamId() {
        return streamId;
    }

    public int getWindowDelta() {
        return windowDelta;
    }

    public boolean isStreamWindowUpdate() {
        return streamId != 0;
    }

    public boolean isSessionWindowUpdate() {
        return streamId == 0;
    }

    @Override
    public String toString() {
        return String.format("%s#%d,delta=%d", super.toString(), streamId, windowDelta);
    }
}
