package com.firefly.net.tcp.nio;

import static com.firefly.net.tcp.TcpPerformanceParameter.CLEANUP_INTERVAL;
import static com.firefly.net.tcp.TcpPerformanceParameter.IO_TIMEOUT_CHECK_INTERVAL;
import static com.firefly.net.tcp.TcpPerformanceParameter.WRITE_SPIN_COUNT;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.ReceiveBufferPool;
import com.firefly.net.ReceiveBufferSizePredictor;
import com.firefly.net.SendBufferPool;
import com.firefly.net.Session;
import com.firefly.net.Worker;
import com.firefly.net.buffer.SocketReceiveBufferPool;
import com.firefly.net.buffer.SocketSendBufferPool;
import com.firefly.net.buffer.SocketSendBufferPool.SendBuffer;
import com.firefly.net.exception.NetException;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public final class TcpWorker implements Worker, Runnable {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final Config config;
	private final Queue<Runnable> registerTaskQueue = new LinkedTransferQueue<Runnable>();
	private final Queue<Runnable> writeTaskQueue = new LinkedTransferQueue<Runnable>();
	private final Queue<SelectionKey> closeTaskQueue = new LinkedTransferQueue<SelectionKey>();
	private final AtomicBoolean wakenUp = new AtomicBoolean();
	private final ReceiveBufferPool receiveBufferPool = new SocketReceiveBufferPool();
	private final SendBufferPool sendBufferPool = new SocketSendBufferPool();
	private final Selector selector;
	private final int workerId;
	final EventManager eventManager;
	
	private volatile int cancelledKeys;
	private Thread thread;
	private boolean start;
	private long lastIoTimeoutCheckTime;

	public TcpWorker(Config config, int workerId, EventManager eventManager) {
		try {
			this.workerId = workerId;
			this.config = config;
			this.eventManager = eventManager;

			selector = Selector.open();
			start = true;
			new Thread(this, "firefly-tcp-worker: " + workerId).start();
		} catch (IOException e) {
			log.error("worker init error", e);
			throw new NetException("worker init error");
		}
	}

	public int getWorkerId() {
		return workerId;
	}

	@Override
	public void registerChannel(Channel channel, int sessionId) {
		SocketChannel socketChannel = (SocketChannel) channel;
		registerTaskQueue.offer(new RegisterTask(socketChannel, sessionId));
		if (wakenUp.compareAndSet(false, true))
			selector.wakeup();
	}

	@Override
	public void run() {
		thread = Thread.currentThread();
		lastIoTimeoutCheckTime = Millisecond100Clock.currentTimeMillis();

		while (start) {
			wakenUp.set(false);
			try {
				select(selector);
				if (wakenUp.get())
					selector.wakeup();

				cancelledKeys = 0;
				processRegisterTaskQueue();
				processWriteTaskQueue();
				processSelectedKeys();
				processCloseTaskQueue();
				processTimeout();
			} catch (Throwable t) {
				log.error("Unexpected exception in the selector loop.", t);

				// Prevent possible consecutive immediate failures that lead to
				// excessive CPU consumption.
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// Ignore.
				}
			}
		}

	}
	
	private void processTimeout() {
		long now = Millisecond100Clock.currentTimeMillis();
		if(now - lastIoTimeoutCheckTime < IO_TIMEOUT_CHECK_INTERVAL)
			return;
		
		for(SelectionKey key : selector.keys()) {
			checkTimeout(key);
		}
		lastIoTimeoutCheckTime = now;
	}
	
	private void checkTimeout(SelectionKey key) {
		TcpSession session = (TcpSession) key.attachment();
		if(!session.isOpen())
			return;
		
		long lastIoTime = Math.max(session.getOpenTime(), session.getLastActiveTime());
		long t = Millisecond100Clock.currentTimeMillis() - lastIoTime;
		if(config.getTimeout() > 0 && t > config.getTimeout()) {
			log.debug("process timeout in select loop|{}|{}", session.getSessionId(), t);
			close0(key);
		}
	}
	
	private void processCloseTaskQueue() throws IOException {
		while (true) {
			SelectionKey selectionKey = closeTaskQueue.poll();
			if (selectionKey == null)
				break;
			
			TcpSession session = (TcpSession) selectionKey.attachment();
			log.debug("process close in queue|{}|{}", session.getSessionId(), session.isOpen());
			close0(selectionKey);
			cleanUpCancelledKeys();
		}

	}
	
	void closeFromUserCode(TcpSession session) {
		if(session.closeTaskInTaskQueue.compareAndSet(false, true)) {
			closeTaskQueue.offer(session.selectionKey);
		}
	}
	
	private void close0(SelectionKey key) {
		TcpSession session = (TcpSession) key.attachment();
		if(!session.isOpen())
			return;
		
		try {
			key.channel().close();
			increaseCancelledKey();
			cleanUpWriteBuffer(session);
			eventManager.executeCloseTask(session);
			session.state = Session.CLOSE;
		} catch (IOException e) {
			log.error("channel close error", e);
		}
	}

	private void processWriteTaskQueue() throws IOException {
		while (true) {
			Runnable task = writeTaskQueue.poll();
			if (task == null)
				break;
			task.run();
			cleanUpCancelledKeys();
		}

	}

	private void processSelectedKeys() throws IOException {
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext();) {
			SelectionKey k = i.next();
			i.remove();
			try {
				int readyOps = k.readyOps();
				if ((readyOps & SelectionKey.OP_READ) != 0 || readyOps == 0) {
					if (!read(k)) {
						// Connection already closed - no need to handle write.
						continue;
					}
				}
				if ((readyOps & SelectionKey.OP_WRITE) != 0) {
					writeFromSelectorLoop(k);
				}
			} catch (CancelledKeyException e) {
				log.debug("processSelectedKeys error close session", e);
				close0(k);
			}

			if (cleanUpCancelledKeys())
				break;
		}

	}

	void writeFromUserCode(final TcpSession session) {
		if (!session.isOpen())
			return;

		if (scheduleWriteIfNecessary(session))
			return;

		// From here, we are sure Thread.currentThread() == workerThread.
		if (session.writeSuspended || session.inWriteNowLoop)
			return;

		log.debug("worker thread write");
		write0(session);
	}

	private boolean scheduleWriteIfNecessary(final TcpSession session) {
		if(Thread.currentThread() == thread)
			return false;
		
		log.debug("schedule write >>>>");
		if (session.writeTaskInTaskQueue.compareAndSet(false, true)) {
			boolean offered = writeTaskQueue.offer(session.writeTask);
			assert offered;
		}
		if (wakenUp.compareAndSet(false, true)) {
			selector.wakeup();
		}
		return true;
	}

	void writeFromTaskLoop(final TcpSession session) {
		if (!session.writeSuspended)
			write0(session);
	}

	private void writeFromSelectorLoop(SelectionKey k) {
		final TcpSession session = (TcpSession) k.attachment();
		session.writeSuspended = false;
		write0(session);
	}

	private void write0(TcpSession session) {
		if (!session.isOpen())
			return;

		boolean open = true;
		boolean addOpWrite = false;
		boolean removeOpWrite = false;
		long writtenBytes = 0;

		final SocketChannel ch = (SocketChannel) session.selectionKey.channel();
		final Queue<Object> writeBuffer = session.writeBuffer;
		final int writeSpinCount = WRITE_SPIN_COUNT;

		session.inWriteNowLoop = true;
		while (true) {
			Object obj = session.currentWrite;
			SendBuffer buf = null;
			if (obj == null) {
				obj = writeBuffer.poll();
				session.currentWrite = obj;
				if (session.currentWrite == null) {
					removeOpWrite = true;
					session.writeSuspended = false;
					break;
				}
				if (obj == Session.CLOSE_FLAG) {
					open = false;
				} else {
					buf = sendBufferPool.acquire(obj);
					session.currentWriteBuffer = buf;
				}
			} else {
				if (obj == Session.CLOSE_FLAG)
					open = false;
				else
					buf = session.currentWriteBuffer;
			}

			try {
				log.debug("0> session is open: {}", open);
				if (!open) {
					log.debug("receive close flag");
					assert buf == null;

					session.resetCurrentWriteAndWriteBuffer();
					buf = null;
					obj = null;
					clearOpWrite(session);
					close0(session.selectionKey);
					break;
				}

				long localWrittenBytes;
				for (int i = writeSpinCount; i > 0; i--) {
					localWrittenBytes = buf.transferTo(ch);
					if (localWrittenBytes != 0) {
						writtenBytes += localWrittenBytes;
						break;
					}
					if (buf.finished()) {
						break;
					}
				}

				if (buf.finished()) {
					// Successful write - proceed to the next message.
					buf.release();
					session.resetCurrentWriteAndWriteBuffer();
					obj = null;
					buf = null;
				} else {
					// Not written fully - perhaps the kernel buffer is
					// full.
					addOpWrite = true;
					session.writeSuspended = true;
					break;
				}
			} catch (AsynchronousCloseException e) {
				// Doesn't need a user attention - ignore.
			} catch (Throwable t) {
				if(buf != null)
					buf.release();
				
				session.resetCurrentWriteAndWriteBuffer();
				buf = null;
				obj = null;
				eventManager.executeExceptionTask(session, t);
				if (t instanceof IOException) {
					log.debug("write0 IOException session close");
					open = false;
					close0(session.selectionKey);
				}
			}
		}
		session.inWriteNowLoop = false;

		// Initially, the following block was executed after releasing
		// the writeLock, but there was a race condition, and it has to be
		// executed before releasing the writeLock:
		if (open) {
			if (addOpWrite) {
				setOpWrite(session);
			} else if (removeOpWrite) {
				clearOpWrite(session);
			}
		}

		if (writtenBytes > 0) {
			session.lastWrittenTime = Millisecond100Clock.currentTimeMillis();
			session.writtenBytes += writtenBytes;
			log.debug("write complete size: {}", writtenBytes);
			log.debug("1> session is open: {}", open);
			log.debug("is in write loop: {}", session.inWriteNowLoop);
		}
	}
	
	private void setOpWrite(TcpSession session) {
		SelectionKey key = session.selectionKey;
		if (key == null) {
			return;
		}
		if (!key.isValid()) {
			log.debug("setOpWrite failure session close");
			close0(key);
			return;
		}

		int interestOps = session.interestOps;
		if ((interestOps & SelectionKey.OP_WRITE) == 0) {
			interestOps |= SelectionKey.OP_WRITE;
			key.interestOps(interestOps);
			session.interestOps = interestOps;
		}
	}

	private void clearOpWrite(TcpSession session) {
		SelectionKey key = session.selectionKey;
		if (key == null) {
			return;
		}
		if (!key.isValid()) {
			log.debug("clearOpWrite key valid false");
			close0(key);
			return;
		}
		
		int interestOps = session.interestOps;
		if ((interestOps & SelectionKey.OP_WRITE) != 0) {
			interestOps &= ~SelectionKey.OP_WRITE;
			log.debug("clear write op >>> {}", interestOps);
			key.interestOps(interestOps);
			session.interestOps = interestOps;
		}
	}

	private void cleanUpWriteBuffer(TcpSession session) {
		Exception cause = null;
		boolean fireExceptionCaught = false;

		// Clean up the stale messages in the write buffer.
		Object obj = session.currentWrite;
		if (obj != null) {
			cause = new NetException("cleanUpWriteBuffer error");
			session.currentWriteBuffer.release();
			session.resetCurrentWriteAndWriteBuffer();
			fireExceptionCaught = true;
		}

		Queue<Object> writeBuffer = session.writeBuffer;
		if (!writeBuffer.isEmpty()) {
			// Create the exception only once to avoid the excessive
			// overhead
			// caused by fillStackTrace.
			if (cause == null) {
				cause = new NetException("cleanUpWriteBuffer error");
			}

			while (true) {
				obj = writeBuffer.poll();
				if (obj == null) {
					break;
				}
				log.warn("error clear obj: {}", obj.getClass().toString());
				fireExceptionCaught = true;
			}
		}

		if (fireExceptionCaught)
			eventManager.executeExceptionTask(session, cause);

	}

	private boolean read(SelectionKey k) {
		final SocketChannel ch = (SocketChannel) k.channel();
		final TcpSession session = (TcpSession) k.attachment();
		final ReceiveBufferSizePredictor predictor = session.receiveBufferSizePredictor;
		final int predictedRecvBufSize = predictor.nextReceiveBufferSize();

		int ret = 0;
		int readBytes = 0;
		boolean failure = true;

		ByteBuffer bb = receiveBufferPool.acquire(predictedRecvBufSize);
		try {
			while ((ret = ch.read(bb)) > 0) {
				readBytes += ret;
				if (!bb.hasRemaining())
					break;
			}
			failure = false;
		} catch (ClosedChannelException e) {
			// Can happen, and does not need a user attention.
		} catch (Throwable t) {
			eventManager.executeExceptionTask(session, t);
		}

		if (readBytes > 0) {
			bb.flip();
			receiveBufferPool.release(bb);

			// Update the predictor.
			predictor.previousReceiveBufferSize(readBytes);
			session.readBytes += readBytes;
			session.lastReadTime = Millisecond100Clock.currentTimeMillis();
			// Decode
			
			try {
				config.getDecoder().decode(bb, session);
			} catch (Throwable t) {
				eventManager.executeExceptionTask(session, t);
			}
			// log.info("Worker {} decode", workerId);
		} else {
			receiveBufferPool.release(bb);
		}

		if (ret < 0 || failure) {
			log.debug("read failure session close");
			k.cancel();
			close0(k);
			return false;
		}

		return true;
	}

	private void processRegisterTaskQueue() throws IOException {
		while (true) {
			Runnable task = registerTaskQueue.poll();
			if (task == null)
				break;
			task.run();
			cleanUpCancelledKeys();
		}
	}

	private final class RegisterTask implements Runnable {

		private SocketChannel socketChannel;
		private int sessionId;

		public RegisterTask(SocketChannel socketChannel, int sessionId) {
			this.socketChannel = socketChannel;
			this.sessionId = sessionId;
		}

		@Override
		public void run() {

			SelectionKey key = null;
			try {
				socketChannel.configureBlocking(false);
				socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
				socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
				socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);

				key = socketChannel.register(selector, SelectionKey.OP_READ);
				TcpSession session = new TcpSession(sessionId, TcpWorker.this, config, Millisecond100Clock.currentTimeMillis(), key);
				key.attach(session);

				SocketAddress localAddress = session.getLocalAddress();
				SocketAddress remoteAddress = session.getRemoteAddress();
				if (localAddress == null || remoteAddress == null)
					TcpWorker.this.close0(key);
				
				eventManager.executeOpenTask(session);
			} catch (IOException e) {
				log.error("socketChannel register error", e);
				close0(key);
			}

		}

	}

	static void select(Selector selector) throws IOException {
		try {
			selector.select(500);
		} catch (CancelledKeyException e) {
			// Harmless exception - log anyway
			log.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector - JDK bug?", e);
		}
	}

	private boolean cleanUpCancelledKeys() throws IOException {
		if (cancelledKeys >= CLEANUP_INTERVAL) {
			cancelledKeys = 0;
			selector.selectNow();
			return true;
		}
		return false;
	}

	private void increaseCancelledKey() {
		int temp = cancelledKeys;
		temp++;
		cancelledKeys = temp;
	}

	@Override
	public void shutdown() {
		eventManager.shutdown();
		start = false;
		log.debug("thread {} is shutdown: {}", thread.getName(), thread.isInterrupted());
	}
}
