package com.firefly.net.tcp.codec.flex.stream.impl;

import com.firefly.net.tcp.codec.flex.encode.MetaInfoGenerator;
import com.firefly.net.tcp.codec.flex.model.MetaInfo;
import com.firefly.net.tcp.codec.flex.protocol.*;
import com.firefly.net.tcp.codec.flex.stream.Stream;
import com.firefly.utils.Assert;
import com.firefly.utils.codec.ByteArrayUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public class FlexOutputStream extends OutputStream implements Callback {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected final MetaInfo metaInfo;
    protected final Stream stream;
    protected final MetaInfoGenerator metaInfoGenerator;

    protected boolean closed;
    protected boolean committed;
    protected boolean noContent = true;

    public FlexOutputStream(MetaInfo metaInfo, Stream stream, MetaInfoGenerator metaInfoGenerator, boolean committed) {
        Assert.notNull(metaInfo, "The meta info must be not null");
        Assert.notNull(stream, "The stream must be not null");

        this.metaInfo = metaInfo;
        this.stream = stream;
        this.metaInfoGenerator = Optional.ofNullable(metaInfoGenerator).orElse(MetaInfoGenerator.DEFAULT);
        this.committed = committed;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized boolean isCommitted() {
        return committed;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] array, int offset, int length) {
        Assert.notNull(array, "The data must be not null");
        write(ByteBuffer.wrap(array, offset, length));
    }

    public synchronized void write(ByteBuffer data) {
        Stream stream = getStream();
        Assert.state(!closed, "The stream " + stream + " output is closed.");

        noContent = false;
        commit();

        if (data.remaining() > Frame.MAX_PAYLOAD_LENGTH) {
            BufferUtils.split(data, Frame.MAX_PAYLOAD_LENGTH)
                       .forEach(buf -> writeFrame(new DataFrame(false, getStream().getId(), false,
                               BufferUtils.toArray(buf))));
        } else {
            writeFrame(new DataFrame(false, getStream().getId(), false, BufferUtils.toArray(data)));
        }
    }

    public synchronized void commit() {
        if (committed || closed) {
            return;
        }

        committed = true;
        byte[] metaInfoData = metaInfoGenerator.generate(metaInfo);
        if (metaInfoData.length > Frame.MAX_PAYLOAD_LENGTH) {
            List<byte[]> splitData = ByteArrayUtils.splitData(metaInfoData, Frame.MAX_PAYLOAD_LENGTH);
            for (int i = 0; i < splitData.size(); i++) {
                boolean endFrame = (i == splitData.size() - 1);
                writeFrame(new ControlFrame(noContent, getStream().getId(), endFrame, splitData.get(i)));
            }
        } else {
            writeFrame(new ControlFrame(noContent, getStream().getId(), true, metaInfoData));
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }

        closed = true;
        commit();
        writeFrame(new DataFrame(true, getStream().getId(), true, null));
    }

    protected synchronized void writeFrame(Frame frame) {
        switch (frame.getType()) {
            case CONTROL:
                ControlFrame controlFrame = (ControlFrame) frame;
                closed = controlFrame.isEndStream();
                getStream().send(controlFrame, this);
                break;
            case DATA:
                DataFrame dataFrame = (DataFrame) frame;
                closed = dataFrame.isEndStream();
                getStream().send(dataFrame, this);
                break;
        }
    }

    @Override
    public void succeeded() {
    }

    @Override
    public synchronized void failed(Throwable x) {
        closed = true;
    }

    public Stream getStream() {
        return stream;
    }
}
