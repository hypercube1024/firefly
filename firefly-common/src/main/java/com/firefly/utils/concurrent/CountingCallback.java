package com.firefly.utils.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * A callback wrapper that succeeds the wrapped callback when the count is
 * reached, or on first failure.
 * </p>
 * <p>
 * This callback is particularly useful when an async operation is split into
 * multiple parts, for example when an original byte buffer that needs to be
 * written, along with a callback, is split into multiple byte buffers, since it
 * allows the original callback to be wrapped and notified only when the last
 * part has been processed.
 * </p>
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 * public void process(EndPoint endPoint, ByteBuffer buffer, Callback callback) {
 * 	ByteBuffer[] buffers = split(buffer);
 * 	CountCallback countCallback = new CountCallback(callback, buffers.length);
 * 	endPoint.write(countCallback, buffers);
 * }
 * </pre>
 */
public class CountingCallback implements Callback {
	private final Callback callback;
	private final AtomicInteger count;

	public CountingCallback(Callback callback, int count) {
		this.callback = callback;
		this.count = new AtomicInteger(count);
	}

	@Override
	public void succeeded() {
		// Forward success on the last success.
		while (true) {
			int current = count.get();

			// Already completed ?
			if (current == 0)
				return;

			if (count.compareAndSet(current, current - 1)) {
				if (current == 1)
					callback.succeeded();
				return;
			}
		}
	}

	@Override
	public void failed(Throwable failure) {
		// Forward failure on the first failure.
		while (true) {
			int current = count.get();

			// Already completed ?
			if (current == 0)
				return;

			if (count.compareAndSet(current, 0)) {
				callback.failed(failure);
				return;
			}
		}
	}

	@Override
	public String toString() {
		return String.format("%s@%x", getClass().getSimpleName(), hashCode());
	}
}
