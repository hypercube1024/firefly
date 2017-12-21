package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.*;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.Assert;
import com.firefly.utils.concurrent.Callback;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Supplier;

import static com.firefly.codec.http2.frame.FrameType.DISCONNECT;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractHTTP2OutputStream extends HTTPOutputStream implements Callback {

    private long size;
    private boolean isWriting;
    private LinkedList<Frame> frames = new LinkedList<>();
    private boolean noContent = true;

    public AbstractHTTP2OutputStream(MetaData info, boolean clientMode) {
        super(info, clientMode);
    }

    @Override
    public synchronized void write(ByteBuffer data) {
        Stream stream = getStream();
        Assert.state(!closed, "The stream " + stream + " output is closed.");

        noContent = false;
        commit();
        writeFrame(new DataFrame(stream.getId(), data, isLastFrame(data)));
    }

    @Override
    public synchronized void commit() {
        if (committed || closed) {
            return;
        }

        HeadersFrame headersFrame = new HeadersFrame(getStream().getId(), info, null, noContent);
        if (log.isDebugEnabled()) {
            log.debug("http2 output stream {} commits the header frame {}", getStream().toString(), headersFrame.toString());
        }
        writeFrame(headersFrame);
        committed = true;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }

        commit();
        if (isChunked()) {
            Optional.ofNullable(info.getTrailerSupplier())
                    .map(Supplier::get)
                    .ifPresent(trailer -> {
                        MetaData metaData = new MetaData(HttpVersion.HTTP_1_1, trailer);
                        HeadersFrame trailerFrame = new HeadersFrame(getStream().getId(), metaData, null, true);
                        frames.offer(trailerFrame);
                    });

            DisconnectFrame disconnectFrame = new DisconnectFrame();
            frames.offer(disconnectFrame);
            if (!isWriting) {
                succeeded();
            }
        }
        closed = true;
    }

    public synchronized void writeFrame(Frame frame) {
        if (isChunked()) {
            frames.offer(frame);
            if (!isWriting) {
                succeeded();
            }
        } else {
            if (isWriting) {
                frames.offer(frame);
            } else {
                _writeFrame(frame);
            }
        }
    }

    @Override
    public synchronized void succeeded() {
        if (isChunked()) {
            if (frames.size() > 2) {
                _writeFrame(frames.poll());
            } else if (frames.size() == 2) {
                Frame frame = frames.getLast();
                if (frame.getType() == DISCONNECT) {
                    Frame lastFrame = frames.poll();
                    frames.clear();
                    switch (lastFrame.getType()) {
                        case DATA: {
                            DataFrame dataFrame = (DataFrame) lastFrame;
                            if (dataFrame.isEndStream()) {
                                _writeFrame(dataFrame);
                            } else {
                                DataFrame lastDataFrame = new DataFrame(dataFrame.getStreamId(), dataFrame.getData(), true);
                                _writeFrame(lastDataFrame);
                            }
                        }
                        break;
                        case HEADERS: {
                            HeadersFrame headersFrame = (HeadersFrame) lastFrame;
                            if (headersFrame.isEndStream()) {
                                _writeFrame(headersFrame);
                            } else {
                                HeadersFrame lastHeadersFrame = new HeadersFrame(headersFrame.getStreamId(),
                                        headersFrame.getMetaData(), headersFrame.getPriority(), true);
                                _writeFrame(lastHeadersFrame);
                            }
                        }
                        break;
                        default:
                            throw new IllegalStateException("The last frame must be data frame or header frame");
                    }
                } else {
                    _writeFrame(frames.poll());
                }
            } else if (frames.size() == 1) {
                Frame frame = frames.getLast();
                if (isLastFrame(frame)) {
                    _writeFrame(frames.poll());
                } else {
                    isWriting = false;
                }
            } else {
                isWriting = false;
            }
        } else {
            Frame frame = frames.poll();
            if (frame != null) {
                _writeFrame(frame);
            } else {
                isWriting = false;
            }
        }
    }

    public boolean isLastFrame(Frame frame) {
        switch (frame.getType()) {
            case HEADERS:
                HeadersFrame headersFrame = (HeadersFrame) frame;
                return headersFrame.isEndStream();
            case DATA:
                DataFrame dataFrame = (DataFrame) frame;
                return dataFrame.isEndStream();
        }
        return false;
    }

    @Override
    public synchronized void failed(Throwable x) {
        frames.clear();
        getStream().getSession().close(ErrorCode.INTERNAL_ERROR.code, "Write frame failure", Callback.NOOP);
        closed = true;
        log.error("Write frame failure", x);
    }

    protected synchronized void _writeFrame(Frame frame) {
//        System.out.println("H2OutputStream writes frame: " + frame);
        isWriting = true;
        switch (frame.getType()) {
            case HEADERS: {
                HeadersFrame headersFrame = (HeadersFrame) frame;
                closed = headersFrame.isEndStream();
                getStream().headers(headersFrame, this);
            }
            break;
            case DATA: {
                DataFrame dataFrame = (DataFrame) frame;
                closed = dataFrame.isEndStream();
                getStream().data(dataFrame, this);
            }
        }
    }

    protected synchronized boolean isLastFrame(ByteBuffer data) {
        long contentLength = getContentLength();
        if (contentLength < 0) {
            return false;
        } else {
            size += data.remaining();
            log.debug("http2 output size: {}, content length: {}", size, contentLength);
            return size >= contentLength;
        }
    }

    protected synchronized long getContentLength() {
        return info.getFields().getLongField(HttpHeader.CONTENT_LENGTH.asString());
    }

    public synchronized boolean isNoContent() {
        return noContent;
    }

    protected synchronized boolean isChunked() {
        return !noContent && getContentLength() < 0;
    }

    abstract protected Stream getStream();

}
