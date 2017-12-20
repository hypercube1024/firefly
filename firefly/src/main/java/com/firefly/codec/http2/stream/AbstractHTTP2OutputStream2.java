package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.Assert;
import com.firefly.utils.concurrent.Callback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractHTTP2OutputStream2 extends HTTPOutputStream implements Callback {

    protected boolean isChunked;
    private long size;
    private boolean isWriting;
    private LinkedList<Frame> frames = new LinkedList<>();
    private boolean noContent;

    public AbstractHTTP2OutputStream2(MetaData info, boolean clientMode, boolean noContent) {
        super(info, clientMode);
        this.noContent = noContent;
    }

    @Override
    public synchronized void write(ByteBuffer data) throws IOException {
        Assert.state(!noContent, "The stream " + getStream() + " is end.");
        commit();

        boolean isLast = isLastFrame(data);
        DataFrame frame = new DataFrame(getStream().getId(), data, isLast);
        writeFrame(frame);
    }

    @Override
    public synchronized void commit() throws IOException {
        if (closed)
            return;

        if (committed)
            return;

        HeadersFrame headersFrame = new HeadersFrame(getStream().getId(), info, null, noContent);
        if (log.isDebugEnabled()) {
            log.debug("http2 output stream {} commits the header frame {}", getStream().toString(), headersFrame.toString());
        }
        writeFrame(headersFrame);
    }

    @Override
    public synchronized void close() throws IOException {
        commit();
    }

    public synchronized void writeFrame(Frame frame) {
        if (isWriting) {
            frames.offer(frame);
        } else {
            _writeFrame(frame);
        }
    }

    @Override
    public synchronized void succeeded() {
        isWriting = false;
        Frame frame = frames.poll();
        if (frame != null) {
            _writeFrame(frame);
        }
    }

    @Override
    public synchronized void failed(Throwable x) {
        isWriting = false;
    }

    public synchronized void _writeFrame(Frame frame) {
        isWriting = true;
        switch (frame.getType()) {
            case HEADERS: {
                getStream().headers((HeadersFrame) frame, this);
            }
            break;
            case DATA: {
                getStream().data((DataFrame) frame, this);
            }
            break;
        }
    }

    public synchronized boolean isLastFrame(ByteBuffer data) {
        long contentLength = info.getFields().getLongField(HttpHeader.CONTENT_LENGTH.asString());
        size += data.remaining();
        log.debug("http2 output size: {}, content length: {}", size, contentLength);
        return size >= contentLength;
    }

    abstract protected Stream getStream();

}
