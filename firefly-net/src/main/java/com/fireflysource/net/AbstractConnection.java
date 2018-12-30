package com.fireflysource.net;

abstract public class AbstractConnection implements Connection {

    protected final int id;
    protected final long openTime;
    protected final long maxIdleTime;
    protected volatile Object attachment;
    protected long closeTime;
    protected long lastReadTime;
    protected long lastWrittenTime;
    protected long readBytes;
    protected long writtenBytes;

    public AbstractConnection(int id, long openTime, long maxIdleTime) {
        this.id = id;
        this.openTime = openTime;
        this.maxIdleTime = maxIdleTime;
    }

    @Override
    public Object getAttachment() {
        return attachment;
    }

    @Override
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public long getOpenTime() {
        return openTime;
    }

    @Override
    public long getCloseTime() {
        return closeTime;
    }

    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    @Override
    public long getLastWrittenTime() {
        return lastWrittenTime;
    }

    @Override
    public long getReadBytes() {
        return readBytes;
    }

    @Override
    public long getWrittenBytes() {
        return writtenBytes;
    }

    @Override
    public long getLastActiveTime() {
        return Math.max(lastReadTime, lastWrittenTime);
    }

    @Override
    public long getIdleTime() {
        return System.currentTimeMillis() - getLastActiveTime();
    }

    @Override
    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    @Override
    public long getDuration() {
        if (isClosed()) {
            return closeTime - openTime;
        } else {
            return System.currentTimeMillis() - openTime;
        }
    }
}
