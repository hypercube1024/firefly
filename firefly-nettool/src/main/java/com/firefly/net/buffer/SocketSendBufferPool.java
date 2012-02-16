package com.firefly.net.buffer;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.WritableByteChannel;
import com.firefly.net.SendBufferPool;

public final class SocketSendBufferPool implements SendBufferPool {
	private static final SendBuffer EMPTY_BUFFER = new EmptySendBuffer();

	private static final int DEFAULT_PREALLOCATION_SIZE = 65536;
	private static final int ALIGN_SHIFT = 4;
	private static final int ALIGN_MASK = 15;

	PreallocationRef poolHead = null;
	Preallocation current = new Preallocation(DEFAULT_PREALLOCATION_SIZE);

	public SocketSendBufferPool() {
		super();
	}

	@Override
	public SendBuffer acquire(Object src) {
		if (src instanceof ByteBuffer) {
			return acquire((ByteBuffer) src);
		} else if (src instanceof FileRegion) {
			return acquire((FileRegion) src);
		}

		throw new IllegalArgumentException("unsupported type: "
				+ src.getClass());
	}

	private final SendBuffer acquire(FileRegion src) {
		if (src.getCount() == 0) {
			return EMPTY_BUFFER;
		}
		return new FileSendBuffer(src);
	}

	private final SendBuffer acquire(ByteBuffer src) {
		final int size = src.remaining();
		if (size == 0) {
			return EMPTY_BUFFER;
		}

		if (src.isDirect()) {
			return new UnpooledSendBuffer(src);
		}
		if (src.remaining() > DEFAULT_PREALLOCATION_SIZE) {
			return new UnpooledSendBuffer(src);
		}

		Preallocation current = this.current;
		ByteBuffer buffer = current.buffer;
		int remaining = buffer.remaining();
		PooledSendBuffer dst;

		if (size < remaining) {
			int nextPos = buffer.position() + size;
			ByteBuffer slice = buffer.duplicate();
			buffer.position(align(nextPos));
			slice.limit(nextPos);
			current.refCnt++;
			dst = new PooledSendBuffer(current, slice);
		} else if (size > remaining) {
			this.current = current = getPreallocation();
			buffer = current.buffer;
			ByteBuffer slice = buffer.duplicate();
			buffer.position(align(size));
			slice.limit(size);
			current.refCnt++;
			dst = new PooledSendBuffer(current, slice);
		} else { // size == remaining
			current.refCnt++;
			this.current = getPreallocation0();
			dst = new PooledSendBuffer(current, current.buffer);
		}

		ByteBuffer dstbuf = dst.buffer;
		dstbuf.mark();
		// src.getBytes(src.readerIndex(), dstbuf);
		dstbuf.put(src.array(), src.position(), Math.min(src.remaining(),
				dstbuf.remaining()));
		dstbuf.reset();
		return dst;
	}

	private final Preallocation getPreallocation() {
		Preallocation current = this.current;
		if (current.refCnt == 0) {
			current.buffer.clear();
			return current;
		}

		return getPreallocation0();
	}

	private final Preallocation getPreallocation0() {
		PreallocationRef ref = poolHead;
		if (ref != null) {
			do {
				Preallocation p = ref.get();
				ref = ref.next;

				if (p != null) {
					poolHead = ref;
					return p;
				}
			} while (ref != null);

			poolHead = ref;
		}

		return new Preallocation(DEFAULT_PREALLOCATION_SIZE);
	}

	private static final int align(int pos) {
		int q = pos >>> ALIGN_SHIFT;
		int r = pos & ALIGN_MASK;
		if (r != 0) {
			q++;
		}
		return q << ALIGN_SHIFT;
	}

	private final class Preallocation {
		final ByteBuffer buffer;
		int refCnt;

		Preallocation(int capacity) {
			buffer = ByteBuffer.allocateDirect(capacity);
		}
	}

	private final class PreallocationRef extends SoftReference<Preallocation> {
		final PreallocationRef next;

		PreallocationRef(Preallocation prealloation, PreallocationRef next) {
			super(prealloation);
			this.next = next;
		}
	}

	public interface SendBuffer {
		boolean finished();

		long writtenBytes();

		long totalBytes();

		long transferTo(WritableByteChannel ch) throws IOException;

		long transferTo(DatagramChannel ch, SocketAddress raddr)
				throws IOException;

		void release();
	}

	class UnpooledSendBuffer implements SendBuffer {

		final ByteBuffer buffer;
		final int initialPos;

		UnpooledSendBuffer(ByteBuffer buffer) {
			this.buffer = buffer;
			initialPos = buffer.position();
		}

		@Override
		public final boolean finished() {
			return !buffer.hasRemaining();
		}

		@Override
		public final long writtenBytes() {
			return buffer.position() - initialPos;
		}

		@Override
		public final long totalBytes() {
			return buffer.limit() - initialPos;
		}

		@Override
		public final long transferTo(WritableByteChannel ch) throws IOException {
			return ch.write(buffer);
		}

		@Override
		public final long transferTo(DatagramChannel ch, SocketAddress raddr)
				throws IOException {
			return ch.send(buffer, raddr);
		}

		@Override
		public void release() {
			// Unpooled.
		}
	}

	final class PooledSendBuffer implements SendBuffer {

		private final Preallocation parent;
		final ByteBuffer buffer;
		final int initialPos;

		PooledSendBuffer(Preallocation parent, ByteBuffer buffer) {
			this.parent = parent;
			this.buffer = buffer;
			initialPos = buffer.position();
		}

		@Override
		public boolean finished() {
			return !buffer.hasRemaining();
		}

		@Override
		public long writtenBytes() {
			return buffer.position() - initialPos;
		}

		@Override
		public long totalBytes() {
			return buffer.limit() - initialPos;
		}

		@Override
		public long transferTo(WritableByteChannel ch) throws IOException {
			return ch.write(buffer);
		}

		@Override
		public long transferTo(DatagramChannel ch, SocketAddress raddr)
				throws IOException {
			return ch.send(buffer, raddr);
		}

		@Override
		public void release() {
			final Preallocation parent = this.parent;
			if (--parent.refCnt == 0) {
				parent.buffer.clear();
				if (parent != current) {
					poolHead = new PreallocationRef(parent, poolHead);
				}
			}
		}
	}

	static final class EmptySendBuffer implements SendBuffer {

		EmptySendBuffer() {
			super();
		}

		@Override
		public final boolean finished() {
			return true;
		}

		@Override
		public final long writtenBytes() {
			return 0;
		}

		@Override
		public final long totalBytes() {
			return 0;
		}

		@Override
		public final long transferTo(WritableByteChannel ch) throws IOException {
			return 0;
		}

		@Override
		public final long transferTo(DatagramChannel ch, SocketAddress raddr)
				throws IOException {
			return 0;
		}

		@Override
		public void release() {
			// Unpooled.
		}
	}

	final class FileSendBuffer implements SendBuffer {

		private final FileRegion file;
		private long writtenBytes;

		FileSendBuffer(FileRegion file) {
			this.file = file;
		}

		@Override
		public boolean finished() {
			return writtenBytes >= file.getCount();
		}

		@Override
		public long writtenBytes() {
			return writtenBytes;
		}

		@Override
		public long totalBytes() {
			return file.getCount();
		}

		@Override
		public long transferTo(WritableByteChannel ch) throws IOException {
			long localWrittenBytes = file.transferTo(ch, writtenBytes);
			writtenBytes += localWrittenBytes;
			return localWrittenBytes;
		}

		@Override
		public long transferTo(DatagramChannel ch, SocketAddress raddr)
				throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void release() {
			file.releaseExternalResources();
		}
	}
}
