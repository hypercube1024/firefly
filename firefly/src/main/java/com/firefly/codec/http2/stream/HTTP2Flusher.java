package com.firefly.codec.http2.stream;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.firefly.codec.common.Callback;
import com.firefly.codec.common.IteratingCallback;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.utils.collection.ArrayQueue;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2Flusher extends IteratingCallback {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final Queue<WindowEntry> windows = new ArrayDeque<>();
	private final ArrayQueue<Entry> frames = new ArrayQueue<>(ArrayQueue.DEFAULT_CAPACITY, ArrayQueue.DEFAULT_GROWTH,
			this);
	private final Map<StreamSPI, Integer> streams = new HashMap<>();
	private final List<Entry> resets = new ArrayList<>();
	private final List<Entry> actives = new ArrayList<>();
	private final Queue<Entry> completes = new ArrayDeque<>();
	private final HTTP2Session session;
	private final Queue<ByteBuffer> buffers = new LinkedList<>();

	public HTTP2Flusher(HTTP2Session session) {
		this.session = session;
	}

	public void window(StreamSPI stream, WindowUpdateFrame frame) {
		boolean added = false;
		synchronized (this) {
			if (!isClosed())
				added = windows.offer(new WindowEntry(stream, frame));
		}
		// Flush stalled data.
		if (added)
			iterate();
	}

	public boolean prepend(Entry entry) {
		boolean fail = false;
		synchronized (this) {
			if (isClosed()) {
				fail = true;
			} else {
				frames.add(0, entry);
				if (log.isDebugEnable())
					log.debug("Prepended {}, frames={}", entry, frames.size());
			}
		}
		if (fail)
			closed(entry, new ClosedChannelException());
		return !fail;
	}

	public boolean append(Entry entry) {
		boolean fail = false;
		synchronized (this) {
			if (isClosed()) {
				fail = true;
			} else {
				frames.offer(entry);
				if (log.isDebugEnable())
					log.debug("Appended {}, frames={}", entry, frames.size());
			}
		}
		if (fail)
			closed(entry, new ClosedChannelException());
		return !fail;
	}

	private Entry remove(int index) {
		synchronized (this) {
			if (index == 0)
				return frames.pollUnsafe();
			else
				return frames.remove(index);
		}
	}

	public int getQueueSize() {
		synchronized (this) {
			return frames.size();
		}
	}

	@Override
	protected Action process() throws Exception {
		if (log.isDebugEnable())
			log.debug("Flushing {}", session);

		synchronized (this) {
			// First thing, update the window sizes, so we can
			// reason about the frames to remove from the queue.
			while (!windows.isEmpty()) {
				WindowEntry entry = windows.poll();
				entry.perform();
			}

			// Now the window sizes cannot change.
			// Window updates that happen concurrently will
			// be queued and processed on the next iteration.
			int sessionWindow = session.getSendWindow();

			int index = 0;
			int size = frames.size();
			while (index < size) {
				Entry entry = frames.get(index);
				StreamSPI stream = entry.stream;

				// If the stream has been reset, don't send the frame.
				if (stream != null && stream.isReset() && !entry.isProtocol()) {
					remove(index);
					--size;
					resets.add(entry);
					if (log.isDebugEnable())
						log.debug("Gathered for reset {}", entry);
					continue;
				}

				// Check if the frame fits in the flow control windows.
				int remaining = entry.dataRemaining();
				if (remaining > 0) {
					FlowControlStrategy flowControl = session.getFlowControlStrategy();
					if (sessionWindow <= 0) {
						flowControl.onSessionStalled(session);
						++index;
						// There may be *non* flow controlled frames to send.
						continue;
					}

					if (stream != null) {
						// The stream may have a smaller window than the
						// session.
						Integer streamWindow = streams.get(stream);
						if (streamWindow == null) {
							streamWindow = stream.updateSendWindow(0);
							streams.put(stream, streamWindow);
						}

						// Is it a frame belonging to an already stalled stream
						// ?
						if (streamWindow <= 0) {
							flowControl.onStreamStalled(stream);
							++index;
							// There may be *non* flow controlled frames to
							// send.
							continue;
						}
					}

					// The frame fits both flow control windows, reduce them.
					sessionWindow -= remaining;
					if (stream != null)
						streams.put(stream, streams.get(stream) - remaining);
				}

				// The frame will be written, remove it from the queue.
				remove(index);
				--size;
				actives.add(entry);

				if (log.isDebugEnable())
					log.debug("Gathered for write {}", entry);
			}
			streams.clear();
		}

		// Perform resets outside the sync block.
		for (int i = 0; i < resets.size(); ++i) {
			Entry entry = resets.get(i);
			entry.reset();
		}
		resets.clear();

		if (actives.isEmpty()) {
			if (isClosed())
				abort(new ClosedChannelException());

			if (log.isDebugEnable())
				log.debug("Flushed {}", session);

			return Action.IDLE;
		}

		synchronized (this) {
			for (int i = 0; i < actives.size(); ++i) {
				Entry entry = actives.get(i);
				Throwable failure = entry.generate(buffers);
				if (failure != null) {
					// Failure to generate the entry is catastrophic.
					failed(failure);
					return Action.SUCCEEDED;
				}
			}

			if (log.isDebugEnable())
				log.debug("Writing {} buffers ({} bytes) for {} frames {}", buffers.size(), getBufferTotalLength(),
						actives.size(), actives);

			ByteBuffer buf = null;
			while ((buf = buffers.poll()) != null) {
				session.getEndPoint().write(buf);
			}
		}
		return Action.SCHEDULED;
	}

	private int getBufferTotalLength() {
		int length = 0;
		for (ByteBuffer buf : buffers) {
			length += buf.remaining();
		}
		return length;
	}

	@Override
	public void succeeded() {
		synchronized (this) {
			buffers.clear();
		}

		// Transfer active items to avoid reentrancy.
		for (int i = 0; i < actives.size(); ++i)
			completes.add(actives.get(i));
		actives.clear();

		if (log.isDebugEnable())
			log.debug("Written {} frames for {}", completes.size(), completes);

		// Drain the frames one by one to avoid reentrancy.
		while (!completes.isEmpty()) {
			Entry entry = completes.poll();
			entry.succeeded();
		}

		super.succeeded();
	}

	@Override
	protected void onCompleteSuccess() {
		throw new IllegalStateException();
	}

	@Override
	protected void onCompleteFailure(Throwable x) {
		if (log.isDebugEnable())
			log.debug("Failed", x);

		synchronized (this) {
			buffers.clear();
		}

		// Transfer active items to avoid reentrancy.
		for (int i = 0; i < actives.size(); ++i)
			completes.add(actives.get(i));
		actives.clear();

		// Drain the frames one by one to avoid reentrancy.
		while (!completes.isEmpty()) {
			Entry entry = completes.poll();
			entry.failed(x);
		}

		abort(x);
	}

	private void abort(Throwable x) {
		Queue<Entry> queued;
		synchronized (this) {
			queued = new ArrayDeque<>(frames);
			frames.clear();
		}

		if (log.isDebugEnable())
			log.debug("Aborting, queued={}", queued.size());

		for (Entry entry : queued)
			closed(entry, x);

		session.abort(x);
	}

	private void closed(Entry entry, Throwable failure) {
		entry.failed(failure);
	}

	public static abstract class Entry implements Callback {
		protected final Frame frame;
		protected final StreamSPI stream;
		protected final Callback callback;

		protected Entry(Frame frame, StreamSPI stream, Callback callback) {
			this.frame = frame;
			this.stream = stream;
			this.callback = callback;
		}

		public int dataRemaining() {
			return 0;
		}

		public Throwable generate(Queue<ByteBuffer> buffers) {
			return null;
		}

		public void reset() {
			failed(new EOFException("reset"));
		}

		@Override
		public void failed(Throwable x) {
			if (stream != null) {
				stream.close();
				stream.getSession().removeStream(stream, true);
			}
			callback.failed(x);
		}

		public boolean isProtocol() {
			switch (frame.getType()) {
			case PRIORITY:
			case RST_STREAM:
			case GO_AWAY:
			case WINDOW_UPDATE:
			case DISCONNECT:
				return true;
			default:
				return false;
			}
		}

		@Override
		public String toString() {
			return frame.toString();
		}
	}

	private class WindowEntry {
		private final StreamSPI stream;
		private final WindowUpdateFrame frame;

		public WindowEntry(StreamSPI stream, WindowUpdateFrame frame) {
			this.stream = stream;
			this.frame = frame;
		}

		public void perform() {
			FlowControlStrategy flowControl = session.getFlowControlStrategy();
			flowControl.onWindowUpdate(session, stream, frame);
		}
	}

}
