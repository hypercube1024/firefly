package com.firefly.net.tcp;

import static com.firefly.net.tcp.TcpPerformanceParameter.WRITE_BUFFER_HIGH_WATER_MARK;
import static com.firefly.net.tcp.TcpPerformanceParameter.WRITE_BUFFER_LOW_WATER_MARK;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.ReceiveBufferSizePredictor;
import com.firefly.net.Session;
import com.firefly.net.buffer.AdaptiveReceiveBufferSizePredictor;
import com.firefly.net.buffer.FileRegion;
import com.firefly.net.buffer.FixedReceiveBufferSizePredictor;
import com.firefly.net.buffer.SocketSendBufferPool.SendBuffer;
import com.firefly.utils.ThreadLocalBoolean;
import com.firefly.utils.collection.LinkedTransferQueue;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.HashTimeWheel;

public final class TcpSession implements Session {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private final int sessionId;
	private final SelectionKey selectionKey;
	private long openTime, lastReadTime, lastWrittenTime, readBytes, writtenBytes;
	private final TcpWorker worker;
	private final Config config;
	private final Map<String, Object> map = new HashMap<String, Object>();
	private final Runnable writeTask = new WriteTask();
	private final AtomicInteger writeBufferSize = new AtomicInteger();
	private final AtomicInteger highWaterMarkCounter = new AtomicInteger();
	private final AtomicBoolean writeTaskInTaskQueue = new AtomicBoolean();
	private InetSocketAddress localAddress;
	private volatile InetSocketAddress remoteAddress;
	private volatile int interestOps = SelectionKey.OP_READ;
	private boolean inWriteNowLoop;
	private boolean writeSuspended;
	private final Object interestOpsLock = new Object();
	private final Object writeLock = new Object();
	private final Queue<Object> writeBuffer = new WriteRequestQueue();
	private Object currentWrite;
	private SendBuffer currentWriteBuffer;
	private volatile int state;
	private ReceiveBufferSizePredictor receiveBufferSizePredictor;
	private EventManager eventManager;
	private HashTimeWheel.Future future;

	public TcpSession(int sessionId, TcpWorker worker, Config config,
			long openTime, SelectionKey selectionKey, EventManager eventManager) {
		super();
		this.sessionId = sessionId;
		this.worker = worker;
		this.config = config;
		this.openTime = openTime;
		this.selectionKey = selectionKey;
		this.eventManager = eventManager;
		if (config.getReceiveByteBufferSize() > 0) {
			log.debug("fix buffer size: {}", config.getReceiveByteBufferSize());
			receiveBufferSizePredictor = new FixedReceiveBufferSizePredictor(
					config.getReceiveByteBufferSize());
		} else {
			log.debug("adaptive buffer size");
			receiveBufferSizePredictor = new AdaptiveReceiveBufferSizePredictor();
		}
		state = OPEN;
	}

	public InetSocketAddress getLocalAddress() {
		if (localAddress == null) {
			SocketChannel socket = (SocketChannel) selectionKey.channel();
			try {
				localAddress = (InetSocketAddress) socket.socket()
						.getLocalSocketAddress();

			} catch (Throwable t) {
				log.error("get localAddress error", t);
			}
		}
		return localAddress;
	}

	public InetSocketAddress getRemoteAddress() {
		if (remoteAddress == null) {
			SocketChannel socket = (SocketChannel) selectionKey.channel();
			try {
				remoteAddress = (InetSocketAddress) socket.socket()
						.getRemoteSocketAddress();
			} catch (Throwable t) {
				log.error("get remoteAddress error", t);
			}
		}
		return remoteAddress;
	}

	ReceiveBufferSizePredictor getReceiveBufferSizePredictor() {
		return receiveBufferSizePredictor;
	}

	AtomicBoolean getWriteTaskInTaskQueue() {
		return writeTaskInTaskQueue;
	}

	public int getState() {
		return state;
	}

	void setState(int state) {
		this.state = state;
	}

	@Override
	public boolean isOpen() {
		return state > 0;
	}

	SendBuffer getCurrentWriteBuffer() {
		return currentWriteBuffer;
	}

	void setCurrentWriteBuffer(SendBuffer currentWriteBuffer) {
		this.currentWriteBuffer = currentWriteBuffer;
	}

	void setCurrentWrite(Object currentWrite) {
		this.currentWrite = currentWrite;
	}

	void resetCurrentWriteAndWriteBuffer() {
		this.currentWrite = null;
		this.currentWriteBuffer = null;
	}

	Object getCurrentWrite() {
		return currentWrite;
	}

	Queue<Object> getWriteBuffer() {
		return writeBuffer;
	}

	Runnable getWriteTask() {
		return writeTask;
	}

	SelectionKey getSelectionKey() {
		return selectionKey;
	}

	Object getInterestOpsLock() {
		return interestOpsLock;
	}

	Object getWriteLock() {
		return writeLock;
	}

	boolean isInWriteNowLoop() {
		return inWriteNowLoop;
	}

	void setInWriteNowLoop(boolean inWriteNowLoop) {
		this.inWriteNowLoop = inWriteNowLoop;
	}

	boolean isWriteSuspended() {
		return writeSuspended;
	}

	void setWriteSuspended(boolean writeSuspended) {
		this.writeSuspended = writeSuspended;
	}

	@Override
	public long getOpenTime() {
		return openTime;
	}

	@Override
	public int getSessionId() {
		return sessionId;
	}

	@Override
	public void setAttribute(String key, Object value) {
		map.put(key, value);
	}

	@Override
	public Object getAttribute(String key) {
		return map.get(key);
	}

	@Override
	public void removeAttribute(String key) {
		map.remove(key);
	}

	@Override
	public void clearAttributes() {
		map.clear();
	}

	@Override
	public void fireReceiveMessage(Object message) {
		worker.getEventManager().executeReceiveTask(this, message);
	}

	@Override
	public void encode(Object message) {
		try {
			config.getEncoder().encode(message, this);
		} catch (Throwable t) {
			eventManager.executeExceptionTask(this, t);
		}
	}
	
	private void write0(Object object) {
		boolean offered = writeBuffer.offer(object);
		assert offered;
		worker.writeFromUserCode(this);
	}
	
	@Override
	public void write(ByteBuffer byteBuffer) {
		write0(byteBuffer);
	}
	
	@Override
	public void write(FileRegion fileRegion) {
		write0(fileRegion);
	}

	int getRawInterestOps() {
		return interestOps;
	}

	void setInterestOpsNow(int interestOps) {
		this.interestOps = interestOps;
	}

	@Override
	public void close(boolean immediately) {
		if (immediately)
			worker.close(selectionKey);
		else
			write0(CLOSE_FLAG);
	}

	private final class WriteTask implements Runnable {

		WriteTask() {
			super();
		}

		@Override
		public void run() {
			writeTaskInTaskQueue.set(false);
			worker.writeFromTaskLoop(TcpSession.this);
		}
	}

	private final class WriteRequestQueue extends LinkedTransferQueue<Object> {
		private static final long serialVersionUID = -2493148252918843163L;
		private final ThreadLocalBoolean notifying = new ThreadLocalBoolean();

		private WriteRequestQueue() {
			super();
		}

		@Override
		public boolean offer(Object object) {
			boolean success = super.offer(object);
			assert success;

			int messageSize = getMessageSize(object);
			int newWriteBufferSize = writeBufferSize.addAndGet(messageSize);
			int highWaterMark = WRITE_BUFFER_HIGH_WATER_MARK;

			if (newWriteBufferSize >= highWaterMark) {
				if (newWriteBufferSize - messageSize < highWaterMark) {
					highWaterMarkCounter.incrementAndGet();
					if (!notifying.get()) {
						notifying.set(Boolean.TRUE);
						worker.setInterestOps(TcpSession.this,
								SelectionKey.OP_READ);
						notifying.set(Boolean.FALSE);
					}
				}
			}
			return true;
		}

		@Override
		public Object poll() {
			Object object = super.poll();
			if (object != null) {
				int messageSize = getMessageSize(object);
				int newWriteBufferSize = writeBufferSize
						.addAndGet(-messageSize);
				int lowWaterMark = WRITE_BUFFER_LOW_WATER_MARK;

				if (newWriteBufferSize == 0
						|| newWriteBufferSize < lowWaterMark) {
					if (newWriteBufferSize + messageSize >= lowWaterMark) {
						highWaterMarkCounter.decrementAndGet();
						if (!notifying.get()) {
							notifying.set(Boolean.TRUE);
							worker.setInterestOps(TcpSession.this,
									SelectionKey.OP_READ);
							notifying.set(Boolean.FALSE);
						}
					}
				}
			}
			return object;
		}

		private int getMessageSize(Object obj) {
			if (obj instanceof ByteBuffer) {
				return ((ByteBuffer) obj).remaining();
			}
			return 0;
		}
	}

	@Override
	public int getInterestOps() {
		if (!isOpen()) {
			return SelectionKey.OP_WRITE;
		}

		int interestOps = getRawInterestOps();
		int writeBufferSize = this.writeBufferSize.get();
		if (writeBufferSize != 0) {
			if (highWaterMarkCounter.get() > 0) {
				int lowWaterMark = WRITE_BUFFER_LOW_WATER_MARK;
				if (writeBufferSize >= lowWaterMark) {
					interestOps |= SelectionKey.OP_WRITE;
				} else {
					interestOps &= ~SelectionKey.OP_WRITE;
				}
			} else {
				int highWaterMark = WRITE_BUFFER_HIGH_WATER_MARK;
				if (writeBufferSize >= highWaterMark) {
					interestOps |= SelectionKey.OP_WRITE;
				} else {
					interestOps &= ~SelectionKey.OP_WRITE;
				}
			}
		} else {
			interestOps &= ~SelectionKey.OP_WRITE;
		}

		return interestOps;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + sessionId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TcpSession other = (TcpSession) obj;
		return sessionId != other.sessionId;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TcpSession");
		sb.append("{sessionId=").append(sessionId);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public long getLastWrittenTime() {
		return lastWrittenTime;
	}

	void setLastWrittenTime(long lastWrittenTime) {
		this.lastWrittenTime = lastWrittenTime;
	}

	@Override
	public long getLastReadTime() {
		return lastReadTime;
	}

	void setLastReadTime(long lastReadTime) {
		this.lastReadTime = lastReadTime;
	}

	@Override
	public long getLastActiveTime() {
		return Math.max(lastReadTime, lastWrittenTime);
	}

	@Override
	public long getReadBytes() {
		return readBytes;
	}

	void setReadBytes(long readBytes) {
		this.readBytes += readBytes;
	}

	@Override
	public long getWrittenBytes() {
		return writtenBytes;
	}

	void setWrittenBytes(long writtenBytes) {
		this.writtenBytes += writtenBytes;
	}

	// TODO need test
	
	@Override
	public void cancelTimeoutTask() {
		if(isCancelTimeoutTask())
			return;
		
		future.cancel();
		future = null;
		log.info("session {} cancel timeout task", getSessionId());
	}

	@Override
	public boolean isCancelTimeoutTask() {
		return future == null;
	}
	
	void setFuture(HashTimeWheel.Future future) {
		this.future = future;
	}

}
