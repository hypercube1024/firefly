package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.DisconnectFrame;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.concurrent.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.function.Supplier;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractHTTP2OutputStream2 extends HTTPOutputStream {
    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected boolean isChunked;
    private long size;
    private long contentLength;
    private boolean isWriting;
    private LinkedList<Frame> frames = new LinkedList<>();
    private FrameCallback frameCallback = new FrameCallback();
    private DataFrame currentDataFrame;

    public AbstractHTTP2OutputStream2(MetaData info, boolean clientMode) {
        super(info, clientMode);
    }

    @Override
    public void commit() throws IOException {
        commit(false);
    }

    @Override
    public synchronized void write(ByteBuffer data) throws IOException {
        if (closed)
            return;

        if (data == null || !data.hasRemaining())
            return;

        if (!committed) {
            commit(false);
        }

        boolean endStream = false;
        if (!isChunked) {
            size += data.remaining();
            log.debug("http2 output size: {}, content length: {}", size, contentLength);
            if (size >= contentLength) {
                endStream = true;
            }
        }

        final Stream stream = getStream();
        final DataFrame frame = new DataFrame(stream.getId(), data, endStream);
        writeFrame(frame);
    }

    public synchronized void writeFrame(Frame frame) {
        switch (frame.getType()) {
            case DATA:
                if (!committed)
                    throw new IllegalStateException("the output stream is not committed");

                DataFrame dataFrame = (DataFrame) frame;
                if (isChunked) {
                    if (dataFrame.isEndStream()) {
                        if (currentDataFrame == null) {
                            writeDataFrame(dataFrame);
                        } else {
                            writeDataFrame(currentDataFrame);
                            writeDataFrame(dataFrame);
                        }
                    } else {
                        if (currentDataFrame == null) {
                            currentDataFrame = dataFrame;
                        } else {
                            writeDataFrame(currentDataFrame);
                            currentDataFrame = dataFrame;
                        }
                    }
                } else {
                    writeDataFrame(dataFrame);
                }
                break;
            case HEADERS:
                writeHeadersFrame((HeadersFrame) frame);
                break;
            case DISCONNECT:
                if (isChunked) {
                    if (currentDataFrame != null) {
                        if (!currentDataFrame.isEndStream()) {
                            final Supplier<HttpFields> trailers = info.getTrailerSupplier();

                            if (trailers == null) {
                                DataFrame theLastDataFrame = new DataFrame(currentDataFrame.getStreamId(), currentDataFrame.getData(), true);
                                writeDataFrame(theLastDataFrame);
                                currentDataFrame = null;
                            } else {
                                DataFrame theLastDataFrame = new DataFrame(currentDataFrame.getStreamId(), currentDataFrame.getData(), false);
                                writeDataFrame(theLastDataFrame);
                                currentDataFrame = null;
                                writeTrailer();
                            }
                        } else {
                            throw new IllegalStateException("the end data stream is cached");
                        }
                    } else {
                        throw new IllegalStateException("the cached data stream is null");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "the frame type is error, only the chunked encoding can accept disconnect frame, current frame type is "
                                    + frame.getType());
                }
                break;
            default:
                throw new IllegalArgumentException("the frame type is error, the type is " + frame.getType());
        }
    }

    protected synchronized void writeDataFrame(DataFrame dataFrame) {
        closed = dataFrame.isEndStream();

        if (isWriting) {
            frames.offer(dataFrame);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("the stream {} writes a frame {}, remaining frames are {}", dataFrame.getStreamId(), dataFrame, frames.toString());
            }
            isWriting = true;
            getStream().data(dataFrame, frameCallback);
        }
    }

    protected synchronized void writeHeadersFrame(HeadersFrame headersFrame) {
        closed = headersFrame.isEndStream();

        if (isWriting) {
            frames.offer(headersFrame);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("the stream {} writes a frame {}", headersFrame.getStreamId(), headersFrame);
            }

            isWriting = true;
            getStream().headers(headersFrame, frameCallback);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed)
            return;

        log.debug("http2 output stream is closing. committed -> {}, isChunked -> {}", committed, isChunked);
        if (!committed) {
            commit(true);
        } else {
            if (isChunked) {
                log.debug("output the last data frame to end stream");
                writeFrame(new DisconnectFrame());
            } else {
                closed = true;
            }
        }
    }

    protected synchronized void commit(final boolean endStream) throws IOException {
        if (closed)
            return;

        if (committed)
            return;

        // does use chunked encoding or content length ?
        contentLength = info.getFields().getLongField(HttpHeader.CONTENT_LENGTH.asString());
        if (endStream) {
            if (log.isDebugEnabled()) {
                log.debug("http2 output stream {} commits header and closes it", getStream().getId());
            }
            isChunked = false;
        } else {
            isChunked = (contentLength <= 0);
        }

        if (log.isDebugEnabled()) {
            log.debug("is http2 output stream {} using chunked encoding ? {}", getStream().getId(), isChunked);
        }

        final Supplier<HttpFields> trailers = info.getTrailerSupplier();
        final Stream stream = getStream();
        committed = true;

        if (trailers == null) {
            HeadersFrame headersFrame = new HeadersFrame(stream.getId(), info, null, endStream);
            if (log.isDebugEnabled()) {
                log.debug("http2 output stream {} commits the header frame {}", stream.getId(), headersFrame);
            }
            writeFrame(headersFrame);
        } else {
            HeadersFrame headersFrame = new HeadersFrame(stream.getId(), info, null, false);
            if (log.isDebugEnabled()) {
                log.debug("http2 output stream {} commits the header frame {}", stream.getId(), headersFrame);
            }
            writeFrame(headersFrame);

            if (endStream) {
                writeTrailer();
            }
        }
    }

    private void writeTrailer() {
        final Supplier<HttpFields> trailers = info.getTrailerSupplier();
        final Stream stream = getStream();
        MetaData trailerMetaData = new MetaData(info.getHttpVersion(), trailers.get());
        HeadersFrame trailer = new HeadersFrame(stream.getId(), trailerMetaData, null, true);
        if (log.isDebugEnabled()) {
            log.debug("http2 output stream {} write the trailer frame {}", stream.getId(), trailer);
        }
        writeFrame(trailer);
    }

    private class FrameCallback implements Callback {

        @Override
        public void succeeded() {
            synchronized (AbstractHTTP2OutputStream2.this) {
                isWriting = false;
                final Frame frame = frames.poll();
                if (frame != null) {
                    switch (frame.getType()) {
                        case DATA:
                            writeDataFrame((DataFrame) frame);
                            break;
                        case HEADERS:
                            writeHeadersFrame((HeadersFrame) frame);
                            break;
                        default:
                            throw new IllegalArgumentException("the frame type is error, the type is " + frame.getType());
                    }
                } else {
                    isWriting = false;
                }

                if (log.isDebugEnabled()) {
                    log.debug("the stream {} outputs http2 frame successfully, and the queue size is {}",
                            getStream().getId(), frames.size());
                }
            }
        }

        @Override
        public void failed(Throwable x) {
            synchronized (AbstractHTTP2OutputStream2.this) {
                log.error("the stream {} outputs http2 frame unsuccessfully ", x, getStream().getId());
                isWriting = false;
            }
        }

    }

    abstract protected Stream getStream();
}
