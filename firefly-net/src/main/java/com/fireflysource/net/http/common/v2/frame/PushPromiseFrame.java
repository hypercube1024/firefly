package com.fireflysource.net.http.common.v2.frame;

import com.fireflysource.net.http.common.model.MetaData;

public class PushPromiseFrame extends Frame {
    private final int streamId;
    private final int promisedStreamId;
    private final MetaData metaData;

    public PushPromiseFrame(int streamId, int promisedStreamId, MetaData metaData) {
        super(FrameType.PUSH_PROMISE);
        this.streamId = streamId;
        this.promisedStreamId = promisedStreamId;
        this.metaData = metaData;
    }

    public int getStreamId() {
        return streamId;
    }

    public int getPromisedStreamId() {
        return promisedStreamId;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public String toString() {
        return String.format("%s#%d/#%d", super.toString(), streamId, promisedStreamId);
    }
}
