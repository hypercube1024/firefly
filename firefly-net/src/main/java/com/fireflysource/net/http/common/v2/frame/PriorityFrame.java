package com.fireflysource.net.http.common.v2.frame;

public class PriorityFrame extends Frame {
    public static final int PRIORITY_LENGTH = 5;

    private final int streamId;
    private final int parentStreamId;
    private final int weight;
    private final boolean exclusive;

    public PriorityFrame(int parentStreamId, int weight, boolean exclusive) {
        this(0, parentStreamId, weight, exclusive);
    }

    public PriorityFrame(int streamId, int parentStreamId, int weight, boolean exclusive) {
        super(FrameType.PRIORITY);
        this.streamId = streamId;
        this.parentStreamId = parentStreamId;
        this.weight = weight;
        this.exclusive = exclusive;
    }

    public int getStreamId() {
        return streamId;
    }

    /**
     * @return <code>int</code> of the Parent Stream
     * @deprecated use {@link #getParentStreamId()} instead.
     */
    @Deprecated
    public int getDependentStreamId() {
        return getParentStreamId();
    }

    public int getParentStreamId() {
        return parentStreamId;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    @Override
    public String toString() {
        return String.format("%s#%d/#%d{weight=%d,exclusive=%b}", super.toString(), streamId, parentStreamId, weight, exclusive);
    }
}
