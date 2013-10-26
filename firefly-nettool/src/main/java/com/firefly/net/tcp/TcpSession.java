package com.firefly.net.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.firefly.net.Config;
import com.firefly.net.ReceiveBufferSizePredictor;
import com.firefly.net.Session;
import com.firefly.net.buffer.AdaptiveReceiveBufferSizePredictor;
import com.firefly.net.buffer.FileRegion;
import com.firefly.net.buffer.FixedReceiveBufferSizePredictor;
import com.firefly.net.buffer.SocketSendBufferPool.SendBuffer;
import com.firefly.utils.collection.LinkedTransferQueue;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public final class TcpSession implements Session {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final int sessionId;
	private final long openTime;
	long lastReadTime, lastWrittenTime, readBytes, writtenBytes;
	private final TcpWorker worker;
	private final Config config;
	private final Map<String, Object> map = new HashMap<String, Object>();
	final Runnable writeTask = new WriteTask();
	final AtomicBoolean writeTaskInTaskQueue = new AtomicBoolean();
	final AtomicBoolean closeTaskInTaskQueue = new AtomicBoolean(false);
	private InetSocketAddress localAddress;
	private volatile InetSocketAddress remoteAddress;
	volatile int interestOps = SelectionKey.OP_READ;
	boolean inWriteNowLoop;
	boolean writeSuspended;
	final SelectionKey selectionKey;
	final Queue<Object> writeBuffer = new LinkedTransferQueue<Object>();
	final ReceiveBufferSizePredictor receiveBufferSizePredictor;
	Object currentWrite;
	SendBuffer currentWriteBuffer;
	volatile int state;
	

	public TcpSession(int sessionId, TcpWorker worker, Config config, long openTime, SelectionKey selectionKey) {
		this.sessionId = sessionId;
		this.worker = worker;
		this.config = config;
		this.openTime = openTime;
		this.selectionKey = selectionKey;

		if (config.getReceiveByteBufferSize() > 0) {
			log.debug("fix buffer size: {}", config.getReceiveByteBufferSize());
			receiveBufferSizePredictor = new FixedReceiveBufferSizePredictor(config.getReceiveByteBufferSize());
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
				localAddress = (InetSocketAddress) socket.socket().getLocalSocketAddress();
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
				remoteAddress = (InetSocketAddress) socket.socket().getRemoteSocketAddress();
			} catch (Throwable t) {
				log.error("get remoteAddress error", t);
			}
		}
		return remoteAddress;
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public boolean isOpen() {
		return state > 0;
	}

	void resetCurrentWriteAndWriteBuffer() {
		this.currentWrite = null;
		this.currentWriteBuffer = null;
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
		worker.eventManager.executeReceiveTask(this, message);
	}

	@Override
	public void encode(Object message) {
		try {
			config.getEncoder().encode(message, this);
		} catch (Throwable t) {
			worker.eventManager.executeExceptionTask(this, t);
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

	@Override
	public void close(boolean immediately) {
		if (immediately)
			worker.closeFromUserCode(this);
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

	@Override
	public long getLastReadTime() {
		return lastReadTime;
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

}
