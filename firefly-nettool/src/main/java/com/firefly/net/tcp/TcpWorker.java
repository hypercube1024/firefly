package com.firefly.net.tcp;

import static com.firefly.net.tcp.TcpPerformanceParameter.CLEANUP_INTERVAL;
import static com.firefly.net.tcp.TcpPerformanceParameter.WRITE_SPIN_COUNT;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
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
import com.firefly.utils.collection.LinkedTransferQueue;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.HashTimeWheel;
import com.firefly.utils.time.Millisecond100Clock;

public final class TcpWorker implements Worker {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static final HashTimeWheel timeWheel = new HashTimeWheel();
	
	private final Config config;
	private final Queue<Runnable> registerTaskQueue = new LinkedTransferQueue<Runnable>();
	private final Queue<Runnable> writeTaskQueue = new LinkedTransferQueue<Runnable>();
	private final AtomicBoolean wakenUp = new AtomicBoolean();
	private final ReceiveBufferPool receiveBufferPool = new SocketReceiveBufferPool();
	private final SendBufferPool sendBufferPool = new SocketSendBufferPool();
	private final Selector selector;
	private final int workerId;
	private volatile int cancelledKeys;
	private Thread thread;
	private EventManager eventManager;
	private boolean start;
	
	static {
		timeWheel.start();
	}

	public TcpWorker(Config config, int workerId, EventManager eventManager) {
		try {
			this.workerId = workerId;
			this.config = config;
			this.eventManager = eventManager;

			selector = Selector.open();
			start = true;
			new Thread(this, "Tcp-worker: " + workerId).start();
		} catch (IOException e) {
			log.error("worker init error", e);
			throw new NetException("worker init error");
		}
	}

	@Override
	public int getWorkerId() {
		return workerId;
	}

	@Override
	public void registerSelectableChannel(SelectableChannel selectableChannel,
			int sessionId) {
		SocketChannel socketChannel = (SocketChannel) selectableChannel;
		registerTaskQueue.offer(new RegisterTask(socketChannel, sessionId));
		if (wakenUp.compareAndSet(false, true))
			selector.wakeup();
	}

	@Override
	public void run() {
		thread = Thread.currentThread();

		while (start) {
			wakenUp.set(false);
			try {
				select(selector);
				if (wakenUp.get())
					selector.wakeup();

				cancelledKeys = 0;
				processRegisterTaskQueue();
				processWriteTaskQueue();
				processSelectedKeys(selector.selectedKeys());
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
	
	EventManager getEventManager() {
		return eventManager;
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

	private void processSelectedKeys(Set<SelectionKey> selectedKeys)
			throws IOException {
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
				close(k);
			}

			if (cleanUpCancelledKeys())
				break;
		}

	}

	void writeFromUserCode(final TcpSession session) {
		if (!session.isOpen()) {
			cleanUpWriteBuffer(session);
			return;
		}

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
		synchronized (session.writeLock) {
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
						// buf = null;
						// obj = null;
						clearOpWrite(session);
						close(session.selectionKey);
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
						close(session.selectionKey);
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
			close(key);
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
			close(key);
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
		synchronized (session.writeLock) {
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
			close(k);
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
				socketChannel.socket().setReuseAddress(true);
				socketChannel.socket().setTcpNoDelay(false);
				socketChannel.socket().setKeepAlive(true);

				key = socketChannel.register(selector, SelectionKey.OP_READ);
				TcpSession session = new TcpSession(sessionId, TcpWorker.this, config, Millisecond100Clock.currentTimeMillis(), key);
				key.attach(session);

				SocketAddress localAddress = session.getLocalAddress();
				SocketAddress remoteAddress = session.getRemoteAddress();
				if (localAddress == null || remoteAddress == null)
					TcpWorker.this.close(key);

				if (config.getTimeout() > 0) {
					HashTimeWheel.Future future = timeWheel.add(config.getTimeout(), new TimeoutTask(session, config.getTimeout()));
					session.setFuture(future);
				}
				
				eventManager.executeOpenTask(session);
			} catch (IOException e) {
				log.error("socketChannel register error", e);
				close(key);
			}

		}

	}

	private final class TimeoutTask implements Runnable {
		private TcpSession session;
		private final long timeout;

		public TimeoutTask(TcpSession session, long timeout) {
			this.session = session;
			this.timeout = timeout;
		}

		@Override
		public void run() {
			long t = Millisecond100Clock.currentTimeMillis() - session.getLastActiveTime();
			if (t >= timeout) {
				session.setFuture(null);
				if(session.isOpen()) {
					eventManager.executeTimeoutTask(session);
				}
			} else {
				long nextCheckTime = timeout - t;
				timeWheel.add(nextCheckTime, TimeoutTask.this);
			}

		}

	}

	void close(SelectionKey key) {
		try {
			key.channel().close();
			increaseCancelledKey();
			TcpSession session = (TcpSession) key.attachment();
			session.state = Session.CLOSE;
			cleanUpWriteBuffer(session);
			// TODO cancel timeout task may lead to performance decline
			session.cancelTimeoutTask(); 
			eventManager.executeCloseTask(session);

		} catch (IOException e) {
			log.error("channel close error", e);
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
		timeWheel.stop();
		log.debug("thread {} is shutdown: {}", thread.getName(), thread.isInterrupted());
	}
}
