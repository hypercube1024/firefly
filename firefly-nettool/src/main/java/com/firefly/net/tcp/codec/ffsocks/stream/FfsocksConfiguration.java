package com.firefly.net.tcp.codec.ffsocks.stream;


import com.firefly.net.tcp.codec.ffsocks.decode.MetaInfoParser;
import com.firefly.net.tcp.codec.ffsocks.encode.MetaInfoGenerator;

/**
 * @author Pengtao Qiu
 */
public class FfsocksConfiguration {

    private MetaInfoParser metaInfoParser;
    private MetaInfoGenerator metaInfoGenerator;
    private int defaultOutputBufferSize = 2 * 1024;

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
}
