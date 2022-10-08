package com.fireflysource.net.http.common.v2.frame;

import com.fireflysource.net.http.common.model.MetaData;

public class HeadersFrame extends Frame {
    private final int streamId;
    private final MetaData metaData;
    private final PriorityFrame priority;
    private final boolean endStream;
    private boolean endHeaders;

    /**
     * <p>Creates a new {@code HEADERS} frame with an unspecified stream {@code id}.</p>
     * <p>The stream {@code id} will be generated by the implementation while sending
     * this frame to the other peer.</p>
     *
     * @param metaData  the metadata containing HTTP request information
     * @param priority  the PRIORITY frame associated with this HEADERS frame
     * @param endStream whether this frame ends the stream
     */
    public HeadersFrame(MetaData metaData, PriorityFrame priority, boolean endStream) {
        this(0, metaData, priority, endStream);
    }

    /**
     * <p>Creates a new {@code HEADERS} frame with the specified stream {@code id}.</p>
     * <p>{@code HEADERS} frames with a specific stream {@code id} are typically used
     * in responses to request {@code HEADERS} frames.</p>
     *
     * @param streamId  the stream id
     * @param metaData  the metadata containing HTTP request/response information
     * @param priority  the PRIORITY frame associated with this HEADERS frame
     * @param endStream whether this frame ends the stream
     */
    public HeadersFrame(int streamId, MetaData metaData, PriorityFrame priority, boolean endStream) {
        super(FrameType.HEADERS);
        this.streamId = streamId;
        this.metaData = metaData;
        this.priority = priority;
        this.endStream = endStream;
    }

    public int getStreamId() {
        return streamId;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public PriorityFrame getPriority() {
        return priority;
    }

    public boolean isEndStream() {
        return endStream;
    }

    public boolean isEndHeaders() {
        return endHeaders;
    }

    public void setEndHeaders(boolean endHeaders) {
        this.endHeaders = endHeaders;
    }

    @Override
    public String toString() {
        return String.format("%s#%d{end=%b}%s", super.toString(), streamId, endStream,
                priority == null ? "" : String.format("+%s", priority));
    }
}