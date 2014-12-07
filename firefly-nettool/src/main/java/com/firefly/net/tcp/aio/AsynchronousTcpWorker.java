package com.firefly.net.tcp.aio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.TimeUnit;

import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.ReceiveBufferSizePredictor;
import com.firefly.net.Session;
import com.firefly.net.Worker;
import com.firefly.net.buffer.ThreadSafeIOBufferPool;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public class AsynchronousTcpWorker implements Worker{
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private ThreadSafeIOBufferPool pool = new ThreadSafeIOBufferPool();
	private final Config config;
	final EventManager eventManager;
	
	public AsynchronousTcpWorker(Config config, EventManager eventManager) {
		this.config = config;
		this.eventManager = eventManager;
	}
	
	@Override
	public void registerChannel(Channel channel, int sessionId) {
		try {
			AsynchronousSocketChannel socketChannel = (AsynchronousSocketChannel)channel;
			socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);
			
			AsynchronousTcpSession session = new AsynchronousTcpSession(sessionId, Millisecond100Clock.currentTimeMillis(), config, this, socketChannel);
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
		final ByteBuffer buf = pool.acquire(predictedRecvBufSize);

		socketChannel.read(buf, config.getTimeout(), TimeUnit.MILLISECONDS, currentSession, new CompletionHandler<Integer, AsynchronousTcpSession>(){

			@Override
			public void completed(Integer readBytes, AsynchronousTcpSession session) {
				session.lastReadTime = Millisecond100Clock.currentTimeMillis();
				if(readBytes <= 0) {
					session.close(true);
					pool.release(buf);
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
					pool.release(buf);
					read(socketChannel, session);
				}
			}

			@Override
			public void failed(Throwable t, AsynchronousTcpSession session) {
				try {
					if(t instanceof InterruptedByTimeoutException) {
						log.debug("reads error, session {} reads data timout", session.getSessionId());
					} else if (t instanceof AsynchronousCloseException) {
						log.debug("reads error, session {} asynchronous close", session.getSessionId());
					} else {
						log.error("socket channel reads error", t);
					}
				} finally {
					pool.release(buf);
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
			socketChannel.write(((ByteBuffer)obj), config.getTimeout(), TimeUnit.MILLISECONDS, currentSession, new CompletionHandler<Integer, AsynchronousTcpSession>(){
				
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
							log.debug("writes error, session {} writes data timout", session.getSessionId());
						} else if (t instanceof AsynchronousCloseException) {
							log.debug("writes error, session {} asynchronous close", session.getSessionId());
						} else {
							log.error("socket channel writes error", t);
						}
					} finally {
						session.close(true);
					}
				}});
		}
		
	}

	@Override
	public void shutdown() {}
	
}
