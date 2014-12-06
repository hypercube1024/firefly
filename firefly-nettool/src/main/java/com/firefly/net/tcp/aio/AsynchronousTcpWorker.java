package com.firefly.net.tcp.aio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.TimeUnit;

import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.ReceiveBufferPool;
import com.firefly.net.ReceiveBufferSizePredictor;
import com.firefly.net.Session;
import com.firefly.net.buffer.SocketReceiveBufferPool;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public class AsynchronousTcpWorker {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final ThreadLocal<ReceiveBufferPool> receiveBufferPool = new ThreadLocal<ReceiveBufferPool>(){
		@Override
		protected ReceiveBufferPool initialValue() {
			return new SocketReceiveBufferPool();
		}
	};
	private final Config config;
	final EventManager eventManager;
	
	public AsynchronousTcpWorker(Config config, EventManager eventManager) {
		this.config = config;
		this.eventManager = eventManager;
	}
	
	public void registerAsynchronousChannel(AsynchronousSocketChannel socketChannel, int sessionId) {
		try {
			socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);
			
			AsynchronousTcpSession session = new AsynchronousTcpSession(sessionId, Millisecond100Clock.currentTimeMillis(), config, this, socketChannel);
//			sessionQueue.offer(session);
			eventManager.executeOpenTask(session);
			read(socketChannel, session);
		} catch(IOException e) {
			log.error("socketChannel register error", e);
		}
	}
	
	public void read(final AsynchronousSocketChannel socketChannel, final AsynchronousTcpSession currentSession) {
		if(!currentSession.isOpen())
			return;
		
		final ReceiveBufferSizePredictor predictor = currentSession.receiveBufferSizePredictor;
		final int predictedRecvBufSize = predictor.nextReceiveBufferSize();
		final ByteBuffer buf = receiveBufferPool.get().acquire(predictedRecvBufSize);

		socketChannel.read(buf, config.getTimeout(), TimeUnit.MILLISECONDS, currentSession, new CompletionHandler<Integer, AsynchronousTcpSession>(){

			@Override
			public void completed(Integer readBytes, AsynchronousTcpSession session) {
				session.lastReadTime = Millisecond100Clock.currentTimeMillis();
				if(readBytes <= 0) {
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
					receiveBufferPool.get().release(buf);
					read(socketChannel, session);
				}
			}

			@Override
			public void failed(Throwable t, AsynchronousTcpSession session) {
				try {
					if(t instanceof InterruptedByTimeoutException) {
						log.info("session {} reads timout", session.getSessionId());
					} else {
						log.error("socket channel reads error", t);
					}
				} finally {
					session.close(true);
				}
			}});
	}
	
	public void write(final AsynchronousSocketChannel socketChannel, final AsynchronousTcpSession currentSession, Object obj) {
		if(!currentSession.isOpen())
			return;
		
		if(obj == Session.CLOSE_FLAG) {
			currentSession.close(true);
			return;
		}
		if(obj instanceof ByteBuffer) {
			socketChannel.write((ByteBuffer)obj, config.getTimeout(), TimeUnit.MILLISECONDS, currentSession, new CompletionHandler<Integer, AsynchronousTcpSession>(){
				
				@Override
				public void completed(Integer writeBytes, AsynchronousTcpSession session) {
					session.lastWrittenTime = Millisecond100Clock.currentTimeMillis();
					if(writeBytes <= 0) {
						session.close(true);
						return;
					}
					session.writtenBytes += writeBytes;
					
					synchronized(session.lock) {
						Object o = session.writeBuffer.poll();
						if(o != null) {
							write(socketChannel, session, o);
						} else {
							session.isWriting = false;
						}
					}
				}

				@Override
				public void failed(Throwable t, AsynchronousTcpSession session) {
					try {
						if(t instanceof InterruptedByTimeoutException) {
							log.info("session {} writes timout", session.getSessionId());
						} else {
							log.error("socket channel writes error", t);
						}
					} finally {
						session.close(true);
					}
				}});
		}
		
	}
	
}
