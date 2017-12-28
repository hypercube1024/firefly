package com.firefly.net.tcp.codec.flex.stream;


import com.firefly.net.tcp.codec.flex.decode.MetaInfoParser;
import com.firefly.net.tcp.codec.flex.encode.MetaInfoGenerator;

/**
 * @author Pengtao Qiu
 */
public class FlexConfiguration {

    private MetaInfoParser metaInfoParser;
    private MetaInfoGenerator metaInfoGenerator;
    private int defaultOutputBufferSize = 2 * 1024;
    private long streamMaxIdleTime = 2 * 60 * 1000;
    private int heartbeatInterval;

    public MetaInfoParser getMetaInfoParser() {
        return metaInfoParser;
    }

    public void setMetaInfoParser(MetaInfoParser metaInfoParser) {
        this.metaInfoParser = metaInfoParser;
    }

    public MetaInfoGenerator getMetaInfoGenerator() {
        return metaInfoGenerator;
    }

    public void setMetaInfoGenerator(MetaInfoGenerator metaInfoGenerator) {
        this.metaInfoGenerator = metaInfoGenerator;
    }

    public int getDefaultOutputBufferSize() {
        return defaultOutputBufferSize;
    }

    public void setDefaultOutputBufferSize(int defaultOutputBufferSize) {
        this.defaultOutputBufferSize = defaultOutputBufferSize;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public long getStreamMaxIdleTime() {
        return streamMaxIdleTime;
    }

    public void setStreamMaxIdleTime(long streamMaxIdleTime) {
        this.streamMaxIdleTime = streamMaxIdleTime;
    }
}
