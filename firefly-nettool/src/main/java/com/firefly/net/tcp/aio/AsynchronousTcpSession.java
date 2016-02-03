package com.firefly.net.tcp.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.ByteBufferOutputEntry;
import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.OutputEntry;
import com.firefly.net.BufferSizePredictor;
import com.firefly.net.Session;
import com.firefly.net.buffer.AdaptiveBufferSizePredictor;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public class AsynchronousTcpSession implements Session {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final int sessionId;
	private final long openTime;
	private long lastReadTime;
	private long lastWrittenTime;
	private long readBytes = 0;
	private long writtenBytes = 0;
	private volatile int state;
	private final AsynchronousSocketChannel socketChannel;
	private volatile InetSocketAddress localAddress;
	private volatile InetSocketAddress remoteAddress;

	private final Config config;
	private final EventManager eventManager;
	private volatile Object attachment;

	private final Lock outputLock = new ReentrantLock();
	private boolean isWriting = false;
	private final Queue<OutputEntry<?>> outputBuffer = new LinkedList<>();
	private final BufferSizePredictor bufferSizePredictor = new AdaptiveBufferSizePredictor();

	public AsynchronousTcpSession(int sessionId, Config config, EventManager eventManager,
			AsynchronousSocketChannel socketChannel) {
		this.sessionId = sessionId;
		this.openTime = Millisecond100Clock.currentTimeMillis();
		this.config = config;
		this.eventManager = eventManager;
		this.socketChannel = socketChannel;
		state = OPEN;
	}

	void _read() {
		if (!isOpen())
			return;

		final int bufferSize = BufferUtils.normalizeBufferSize(bufferSizePredictor.nextBufferSize());
		final ByteBuffer buf = ByteBuffer.allocate(bufferSize);

		if (log.isDebugEnabled()) {
			log.debug("the session {} buffer size is {}", getSessionId(), bufferSize);
		}
		socketChannel.read(buf, config.getTimeout(), TimeUnit.MILLISECONDS, this,
				new CompletionHandler<Integer, AsynchronousTcpSession>() {

					@Override
					public void completed(Integer currentReadBytes, AsynchronousTcpSession session) {
						session.lastReadTime = Millisecond100Clock.currentTimeMillis();
						if (currentReadBytes < 0) {
							if (log.isDebugEnabled()) {
								log.debug("the session {} input is closed, {}", session.getSessionId(),
										currentReadBytes);
							}
							session.closeNow();
							return;
						}

						if (log.isDebugEnabled()) {
							log.debug("the session {} read {} bytes", session.getSessionId(), currentReadBytes);
						}
						// Update the predictor.
						session.bufferSizePredictor.previousReceivedBufferSize(currentReadBytes);
						session.readBytes += currentReadBytes;

						buf.flip();
						try {
							config.getDecoder().decode(buf, session);
						} catch (Throwable t) {
							eventManager.executeExceptionTask(session, t);
						} finally {
							_read();
						}
					}

					@Override
					public void failed(Throwable t, AsynchronousTcpSession session) {
						if (t instanceof InterruptedByTimeoutException) {
							if (log.isDebugEnabled()) {
								log.debug("the session {} reading data is timeout.", getSessionId());
							}
						} else {
							log.error("the session {} read data is failed", t, session.getSessionId());
						}

						session.closeNow();
					}
				});
	}

	private void writingFailedCallback(Callback callback, Throwable t) {
		if (t instanceof InterruptedByTimeoutException) {
			if (log.isDebugEnabled()) {
				log.debug("the session {} writing data is timeout.", getSessionId());
			}
		} else {
			log.error("the session {} writes data is failed", t, getSessionId());
		}
		closeNow();
		callback.failed(t);
	}

	private void writingCompletedCallback(Callback callback, long currentWritenBytes) {
		lastWrittenTime = Millisecond100Clock.currentTimeMillis();
		if (currentWritenBytes < 0) {
			if (log.isDebugEnabled()) {
				log.debug("the session {} output is closed, {}", getSessionId(), currentWritenBytes);
			}
			closeNow();
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug("the session {} writes {} bytes", getSessionId(), currentWritenBytes);
		}

		writtenBytes += currentWritenBytes;
		callback.succeeded();

		outputLock.lock();
		try {
			OutputEntry<?> obj = outputBuffer.poll();
			if (obj != null) {
				_write(obj);
			} else {
				isWriting = false;
			}
		} finally {
			outputLock.unlock();
		}
	}

	void _write(final OutputEntry<?> entry) {
		if (!isOpen())
			return;

		switch (entry.getOutputEntryType()) {
		case BYTE_BUFFER:
			ByteBufferOutputEntry byteBufferOutputEntry = (ByteBufferOutputEntry) entry;
			if (log.isDebugEnabled()) {
				log.debug("the session {} will write buffer {}", getSessionId(),
						byteBufferOutputEntry.getData().remaining());
			}
			socketChannel.write(byteBufferOutputEntry.getData(), config.getTimeout(), TimeUnit.MILLISECONDS, this,
					new CompletionHandler<Integer, AsynchronousTcpSession>() {

						@Override
						public void completed(Integer currentWritenBytes, AsynchronousTcpSession session) {
							writingCompletedCallback(entry.getCallback(), currentWritenBytes);
						}

						@Override
						public void failed(Throwable t, AsynchronousTcpSession session) {
							writingFailedCallback(entry.getCallback(), t);
						}
					});
			break;

		case BYTE_BUFFER_ARRAY:
			ByteBufferArrayOutputEntry byteBuffersEntry = (ByteBufferArrayOutputEntry) entry;
			socketChannel.write(byteBuffersEntry.getData(), 0, byteBuffersEntry.getData().length, config.getTimeout(),
					TimeUnit.MILLISECONDS, this, new CompletionHandler<Long, AsynchronousTcpSession>() {

						@Override
						public void completed(Long currentWritenBytes, AsynchronousTcpSession session) {
							writingCompletedCallback(entry.getCallback(), currentWritenBytes);
						}

						@Override
						public void failed(Throwable t, AsynchronousTcpSession session) {
							writingFailedCallback(entry.getCallback(), t);
						}
					});
			break;
		case DISCONNECTION:
			log.debug("the session {} will close", getSessionId());
			shutdownSocketChannel();
		default:
			break;
		}
	}

	@Override
	public void attachObject(Object attachment) {
		this.attachment = attachment;
	}

	@Override
	public Object getAttachment() {
		return attachment;
	}

	@Override
	public void fireReceiveMessage(Object message) {
		eventManager.executeReceiveTask(this, message);
	}

	@Override
	public void encode(Object message) {
		try {
			config.getEncoder().encode(message, this);
		} catch (Throwable t) {
			eventManager.executeExceptionTask(this, t);
		}
	}

	@Override
	public void write(OutputEntry<?> entry) {
		if (entry == null)
			return;

		outputLock.lock();
		try {
			if (!isWriting) {
				isWriting = true;
				_write(entry);
			} else {
				outputBuffer.offer(entry);
			}
		} finally {
			outputLock.unlock();
		}
	}

	@Override
	public void write(ByteBuffer byteBuffer, Callback callback) {
		write(new ByteBufferOutputEntry(callback, byteBuffer));
	}

	@Override
	public void write(ByteBuffer[] buffers, Callback callback) {
		write(new ByteBufferArrayOutputEntry(callback, buffers));
	}

	@Override
	public void write(Collection<ByteBuffer> buffers, Callback callback) {
		write(new ByteBufferArrayOutputEntry(callback, buffers.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY)));
	}

	@Override
	public void write(FileRegion file, Callback callback) {
		try {
			transferTo(file.getFile(), file.getPosition(), file.getCount(), callback);
		} catch (Throwable t) {
			log.error("transfer file error", t);
		} finally {
			file.releaseExternalResources();
		}
	}

	public long transferTo(FileChannel fc, long pos, long len, Callback callback) throws Throwable {
		long ret = 0;
		long bufferSize = 1024 * 8;
		long bufferCount = (len + bufferSize - 1) / bufferSize;
		CountingCallback countingCallback = new CountingCallback(callback, (int) bufferCount);
		try {
			ByteBuffer buf = ByteBuffer.allocate((int) bufferSize);
			int i = 0;
			while ((i = fc.read(buf, pos)) != -1) {
				if (i > 0) {
					ret += i;
					pos += i;
					buf.flip();
					write(buf, countingCallback);
					buf = ByteBuffer.allocate((int) bufferSize);
				}
				if (log.isDebugEnabled()) {
					log.debug("write file ret {} | len {}", ret, len);
				}
				if (ret >= len)
					break;
			}
		} finally {
			fc.close();
		}
		return ret;
	}

	@Override
	public void close() {
		write(DISCONNECTION_FLAG);
	}

	@Override
	public void closeNow() {
		try {
			socketChannel.close();
		} catch (AsynchronousCloseException e) {
			if (log.isDebugEnabled())
				log.debug("the session {} asynchronously closed", sessionId);
		} catch (IOException e) {
			log.error("the session {} close error", e, sessionId);
		}
		state = CLOSE;
		eventManager.executeCloseTask(this);
	}
	
	@Override
	public void shutdownOutput() {
		try {
			socketChannel.shutdownOutput();
		} catch (ClosedChannelException e) {
			log.debug("the session {} is closed", e, sessionId);
		} catch (IOException e) {
			log.error("the session {} shutdown output error", e, sessionId);
		}
	}
	
	@Override
	public void shutdownInput() {
		try {
			socketChannel.shutdownInput();
		} catch (ClosedChannelException e) {
			log.debug("the session {} is closed", e, sessionId);
		} catch (IOException e) {
			log.error("the session {} shutdown input error", e, sessionId);
		}
	}

	private void shutdownSocketChannel() {
		shutdownOutput();
		shutdownInput();
	}

	@Override
	public int getSessionId() {
		return sessionId;
	}

	@Override
	public long getOpenTime() {
		return openTime;
	}

	@Override
	public long getLastReadTime() {
		return lastReadTime;
	}

	@Override
	public long getLastWrittenTime() {
		return lastWrittenTime;
	}

	@Override
	public long getLastActiveTime() {
		return Math.max(lastReadTime, lastWrittenTime);
	}

	@Override
	public long getReadBytes() {
		return readBytes;
	}

	@Override
	public long getWrittenBytes() {
		return writtenBytes;
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public boolean isOpen() {
		return state > 0;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		if(localAddress != null) {
			return localAddress;
		} else {
			try {
				localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
				return localAddress;
			} catch (IOException e) {
				log.error("the session {} gets local address error", e, sessionId);
				return null;
			}
		}
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		if(remoteAddress != null) {
			return remoteAddress;
		} else {
			try {
				remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
				return remoteAddress;
			} catch (Throwable t) {
				log.error("the session {} gets remote address error", t, sessionId);
				return null;
			}
		}
	}

}
