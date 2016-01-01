package com.firefly.codec.http2.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

abstract public class AbstractHTTP2OutputStream extends HTTPOutputStream {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	protected boolean isChunked;

	private boolean isWriting;
	private LinkedList<Frame> frames = new LinkedList<>();
	private FrameCallback frameCallback = new FrameCallback();

	public AbstractHTTP2OutputStream(MetaData info, boolean clientMode) {
		super(info, clientMode);
		commited = clientMode;
	}

	@Override
	public synchronized void writeWithContentLength(ByteBuffer[] data) throws IOException {
		try {
			if (!commited) {
				long contentLength = 0;
				for (ByteBuffer buf : data) {
					contentLength += buf.remaining();
				}
				info.getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(contentLength));
			}

			final int last = data.length - 1;
			for (int i = 0; i < data.length; i++) {
				write(data[i], i == last);
			}
		} finally {
			close();
		}
	}

	@Override
	public synchronized void writeWithContentLength(ByteBuffer data) throws IOException {
		try {
			if (!commited) {
				info.getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(data.remaining()));
			}

			write(data, true);
		} finally {
			close();
		}
	}

	@Override
	public void commit() throws IOException {
		commit(null, false);
	}

	@Override
	public void write(ByteBuffer data) throws IOException {
		write(data, false);
	}

	public synchronized void writeFrame(Frame frame) {
		if (frame instanceof DataFrame) {
			DataFrame dataFrame = (DataFrame) frame;
			closed = dataFrame.isEndStream();

			if (isWriting) {
				frames.offer(dataFrame);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("the stream {} writes a frame {}", dataFrame.getStreamId(), dataFrame);
				}

				isWriting = true;
				getStream().data(dataFrame, frameCallback);
			}

		} else if (frame instanceof HeadersFrame) {
			HeadersFrame headersFrame = (HeadersFrame) frame;
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

		} else {
			throw new IllegalArgumentException("the frame type is error, " + frame.getClass());
		}
	}

	@Override
	public synchronized void close() throws IOException {
		if (closed)
			return;

		log.debug("http2 output stream is closing");
		if (!commited) {
			commit(null, true);
		} else {
			if (isChunked) {
				String trailerName = info.getFields().get(HttpHeader.TRAILER);
				if (VerifyUtils.isNotEmpty(trailerName)) {
					final Stream stream = getStream();
					MetaData trailer = new MetaData(null, new HttpFields());

					String trailerValue = info.getFields().get("Trailer-Value");
					if (VerifyUtils.isNotEmpty(trailerValue)) {
						trailer.getFields().add(trailerName, trailerValue);
					} else {
						trailer.getFields().add(trailerName, "end");
					}

					if (log.isDebugEnabled()) {
						log.debug("the stream {} will write trailer {}", getStream().getId(), trailer);
					}

					final HeadersFrame chunkedTrailerFrame = new HeadersFrame(stream.getId(), trailer, null, true);
					writeFrame(chunkedTrailerFrame);
				} else {
					// TODO how to avoid to output an empty data frame 
					if (log.isDebugEnabled()) {
						log.debug("output a empty data frame to end stream");
					}
					write(BufferUtils.EMPTY_BUFFER, true);
				}
			} else {
				closed = true;
			}
		}
	}

	protected synchronized void commit(final ByteBuffer data, final boolean endStream) throws IOException {
		if (closed)
			return;

		if (commited)
			return;

		// does use chunked encoding or content length ?
		if (data != null) {
			if (endStream) {
				if (log.isDebugEnabled()) {
					log.debug("stream {} commits data with content length {} and closes it", getStream().getId(),
							data.remaining());
				}
				info.getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(data.remaining()));
				isChunked = false;
			} else {
				if (info.getFields().contains(HttpHeader.CONTENT_LENGTH)) {
					if (log.isDebugEnabled()) {
						log.debug("stream {} commits data and the header contains content length {}",
								getStream().getId(), info.getFields().get(HttpHeader.CONTENT_LENGTH));
					}
					isChunked = false;
				} else {
					if (log.isDebugEnabled()) {
						log.debug("stream {} commits data using chunked encoding", getStream().getId());
					}
					isChunked = true;
				}
			}
		} else {
			if (endStream) {
				if (log.isDebugEnabled()) {
					log.debug("stream {} commits header and closes it", getStream().getId());
				}
				isChunked = false;
			} else {
				if (info.getFields().contains(HttpHeader.CONTENT_LENGTH)) {
					if (log.isDebugEnabled()) {
						log.debug("stream {} commits header that contains content length {}", getStream().getId(),
								info.getFields().get(HttpHeader.CONTENT_LENGTH));
					}
					isChunked = false;
				} else {
					if (log.isDebugEnabled()) {
						log.debug("stream {} commits header using chunked encoding", getStream().getId());
					}
					isChunked = true;
				}
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("is stream {} using chunked encoding ? {}", getStream().getId(), isChunked);
		}

		final Stream stream = getStream();
		final HeadersFrame headersFrame = new HeadersFrame(stream.getId(), info, null, data == null && endStream);
		if (log.isDebugEnabled()) {
			log.debug("stream {} commits the header frame {}", stream.getId(), headersFrame);
		}

		commited = true;
		writeFrame(headersFrame);
		if (data != null) {
			write(data, endStream);
		}
	}

	@Override
	public synchronized void write(ByteBuffer data, boolean endStream) throws IOException {
		if (closed)
			return;

		if (!commited) {
			commit(data, endStream);
		} else {
			final Stream stream = getStream();
			final DataFrame frame = new DataFrame(stream.getId(), data, endStream);
			writeFrame(frame);
		}
	}

	private class FrameCallback implements Callback {

		@Override
		public void succeeded() {
			synchronized (AbstractHTTP2OutputStream.this) {
				isWriting = false;
				final Frame frame = frames.poll();
				if (frame != null) {
					writeFrame(frame);
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
			synchronized (AbstractHTTP2OutputStream.this) {
				log.error("the stream {} outputs http2 frame unsuccessfully ", x, getStream().getId());
				isWriting = false;
			}
		}

	}

	abstract protected Stream getStream();
}
