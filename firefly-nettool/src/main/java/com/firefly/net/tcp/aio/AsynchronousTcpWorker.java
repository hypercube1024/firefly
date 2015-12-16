package com.firefly.net.tcp.aio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.TimeUnit;

import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.ByteBufferOutputEntry;
import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.OutputEntry;
import com.firefly.net.ReceiveBufferSizePredictor;
import com.firefly.net.Worker;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public class AsynchronousTcpWorker implements Worker {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final Config config;
	final EventManager eventManager;

	public AsynchronousTcpWorker(Config config, EventManager eventManager) {
		this.config = config;
		this.eventManager = eventManager;
	}

	@Override
	public void registerChannel(Channel channel, int sessionId) {
		try {
			AsynchronousSocketChannel socketChannel = (AsynchronousSocketChannel) channel;
			socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);

			AsynchronousTcpSession session = new AsynchronousTcpSession(sessionId,
					Millisecond100Clock.currentTimeMillis(), config, this, socketChannel);
			eventManager.executeOpenTask(session);
			read(socketChannel, session);
		} catch (IOException e) {
			log.error("socketChannel register error", e);
		}
	}

	public void read(final AsynchronousSocketChannel socketChannel, final AsynchronousTcpSession currentSession) {
		if (!currentSession.isOpen())
			return;

		final ReceiveBufferSizePredictor predictor = currentSession.receiveBufferSizePredictor;
		final int predictedRecvBufSize = predictor.nextReceiveBufferSize();
		final ByteBuffer buf = ByteBuffer.allocate(predictedRecvBufSize);

		if (log.isDebugEnable()) {
			log.debug("current socket channel is open {} for input", socketChannel.isOpen());
		}
		socketChannel.read(buf, config.getTimeout(), TimeUnit.MILLISECONDS, currentSession,
				new CompletionHandler<Integer, AsynchronousTcpSession>() {

					@Override
					public void completed(Integer readBytes, AsynchronousTcpSession session) {
						session.lastReadTime = Millisecond100Clock.currentTimeMillis();
						if (readBytes < 0) {
							if (log.isDebugEnable()) {
								log.debug("The channel {} input is closed, {}", session.getSessionId(), readBytes);
							}
							session.close(true);
							return;
						}
						buf.flip();
						// Update the predictor.
						predictor.previousReceiveBufferSize(readBytes);
						session.readBytes += readBytes;
						session.lastReadTime = Millisecond100Clock.currentTimeMillis();
						try {
							config.getDecoder().decode(buf, session);
						} catch (Throwable t) {
							eventManager.executeExceptionTask(session, t);
						} finally {
							read(socketChannel, session);
						}
					}

					@Override
					public void failed(Throwable t, AsynchronousTcpSession session) {
						if (t instanceof InterruptedByTimeoutException) {
							if (log.isDebugEnable()) {
								log.debug("session {} reads data timout", session.getSessionId());
							}
						} else {
							log.error("socket channel reads error", t);
						}
						
						session.shutdownSocketChannel();
					}
				});
	}

	private void writingFailedCallback(final AsynchronousSocketChannel socketChannel, final OutputEntry<?> entry,
			Throwable t, AsynchronousTcpSession session) {
		if (t instanceof InterruptedByTimeoutException) {
			if (log.isDebugEnable()) {
				log.debug("session {} writes data timout", session.getSessionId());
			}
		} else {
			log.error("socket channel writes error", t);
		}
		session.shutdownSocketChannel();
		entry.getCallback().failed(t);
	}

	private void writingCompletedCallback(final AsynchronousSocketChannel socketChannel, final OutputEntry<?> entry,
			long writeBytes, AsynchronousTcpSession session) {
		session.lastWrittenTime = Millisecond100Clock.currentTimeMillis();
		if (writeBytes < 0) {
			if (log.isDebugEnable()) {
				log.debug("The channel {} output is closed, {}", session.getSessionId(), writeBytes);
			}
			session.shutdownSocketChannel();
			return;
		}
		session.writtenBytes += writeBytes;

		entry.getCallback().succeeded();

		synchronized (session.lock) {
			OutputEntry<?> obj = session.writeBuffer.poll();
			if (obj != null) {
				write(socketChannel, session, obj);
			} else {
				session.isWriting = false;
			}
		}
	}

	public void write(final AsynchronousSocketChannel socketChannel, final AsynchronousTcpSession currentSession,
			final OutputEntry<?> entry) {
		if (!currentSession.isOpen())
			return;

		switch (entry.getOutputEntryType()) {
		case BYTE_BUFFER:
			ByteBufferOutputEntry byteBufferOutputEntry = (ByteBufferOutputEntry) entry;
			if (log.isDebugEnable()) {
				log.debug("socket channel will write buffer {}", byteBufferOutputEntry.getData().remaining());
			}
			socketChannel.write(byteBufferOutputEntry.getData(), config.getTimeout(), TimeUnit.MILLISECONDS,
					currentSession, new CompletionHandler<Integer, AsynchronousTcpSession>() {

						@Override
						public void completed(Integer writeBytes, AsynchronousTcpSession session) {
							writingCompletedCallback(socketChannel, entry, writeBytes, currentSession);
						}

						@Override
						public void failed(Throwable t, AsynchronousTcpSession session) {
							writingFailedCallback(socketChannel, entry, t, currentSession);
						}
					});
			break;

		case BYTE_BUFFER_ARRAY:
			ByteBufferArrayOutputEntry byteBuffersEntry = (ByteBufferArrayOutputEntry) entry;
			socketChannel.write(byteBuffersEntry.getData(), 0, byteBuffersEntry.getData().length, config.getTimeout(),
					TimeUnit.MILLISECONDS, currentSession, new CompletionHandler<Long, AsynchronousTcpSession>() {

						@Override
						public void completed(Long writeBytes, AsynchronousTcpSession session) {
							writingCompletedCallback(socketChannel, entry, writeBytes, currentSession);
						}

						@Override
						public void failed(Throwable t, AsynchronousTcpSession session) {
							writingFailedCallback(socketChannel, entry, t, currentSession);
						}
					});
			break;
		case DISCONNECTION:
			log.debug("the socket channel {} will close", currentSession.getSessionId());
			currentSession.shutdownSocketChannel();
		default:
			break;
		}
	}

	@Override
	public void shutdown() {
	}

}
