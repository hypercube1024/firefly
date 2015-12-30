package com.firefly.codec.http2.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

abstract public class AbstractHTTP2OutputStream extends HTTPOutputStream {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	private StreamCallback callback = new StreamCallback();
	private boolean isWriting;
	private LinkedList<DataFrame> dataFrames = new LinkedList<>();

	public AbstractHTTP2OutputStream(MetaData info, boolean clientMode) {
		super(info, clientMode);
	}

	@Override
	public void writeWithContentLength(ByteBuffer[] data) throws IOException {
		try {
			final int last = data.length - 1;
			for (int i = 0; i < data.length; i++) {
				write(data[i], i == last);
			}
		} finally {
			close();
		}
	}

	@Override
	public void writeWithContentLength(ByteBuffer data) throws IOException {
		try {
			write(data, true);
		} finally {
			close();
		}
	}

	@Override
	public void commit() throws IOException {
		commit(false);
	}

	@Override
	public void write(ByteBuffer data) throws IOException {
		write(data, false);
	}

	@Override
	public synchronized void close() throws IOException {
		// TODO
		if (closed)
			return;

		log.debug("http2 output stream is closing");
		if (!commited) {
			commit(true);
		} else {
			write(ByteBuffer.allocate(0), true);
		}
	}

	protected synchronized void commit(boolean endStream) throws IOException {
		if (closed)
			return;

		if (commited)
			return;

		final Stream stream = getStream();
		final HeadersFrame frame = new HeadersFrame(stream.getId(), info, null, endStream);
		if(log.isDebugEnabled()) {
			log.debug("stream {} commits the header frame {}", stream.getId(), frame);
		}
		isWriting = true;
		stream.headers(frame, callback);

		commited = true;
		closed = endStream;
	}

	public synchronized void write(ByteBuffer data, boolean endStream) throws IOException {
		if (closed)
			return;

		if (!commited) {
			commit();
		} else {
			final Stream stream = getStream();
			final DataFrame frame = new DataFrame(stream.getId(), data, endStream);
			if (isWriting) {
				dataFrames.offer(frame);
			} else {
				isWriting = true;
				stream.data(frame, callback);
			}
			closed = frame.isEndStream();
		}
	}

	private class StreamCallback implements Callback {

		@Override
		public void succeeded() {
			synchronized (AbstractHTTP2OutputStream.this) {
				if (log.isDebugEnabled()) {
					log.debug("the stream {} outputs http2 frame successfully", getStream().getId());
				}

				final Stream stream = getStream();
				DataFrame frame = dataFrames.poll();
				if (frame != null) {
					isWriting = true;
					stream.data(frame, callback);
					closed = frame.isEndStream();
				} else {
					isWriting = false;
				}
			}
		}

		@Override
		public void failed(Throwable x) {
			synchronized (AbstractHTTP2OutputStream.this) {
				if (log.isDebugEnabled()) {
					log.error("the stream {} outputs http2 frame unsuccessfully ", x, getStream().getId());
				}
				isWriting = false;
				closed = true;
			}
		}

	}

	abstract protected Stream getStream();
}
