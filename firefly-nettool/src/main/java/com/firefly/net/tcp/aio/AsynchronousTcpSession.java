package com.firefly.net.tcp.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.Queue;

import com.firefly.net.Config;
import com.firefly.net.ReceiveBufferSizePredictor;
import com.firefly.net.Session;
import com.firefly.net.buffer.AdaptiveReceiveBufferSizePredictor;
import com.firefly.net.buffer.FileRegion;
import com.firefly.net.buffer.FixedReceiveBufferSizePredictor;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class AsynchronousTcpSession implements Session {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final int sessionId;
	private final long openTime;
	long lastReadTime, lastWrittenTime, readBytes, writtenBytes;
	private final Config config;
	private final AsynchronousTcpWorker worker;
	private Object attachment;
	final AsynchronousSocketChannel socketChannel;
	private volatile InetSocketAddress localAddress;
	private volatile InetSocketAddress remoteAddress;
	volatile int state;
	final Queue<Object> writeBuffer = new LinkedList<Object>();
	final ReceiveBufferSizePredictor receiveBufferSizePredictor;
	boolean isWriting = false;
	Object lock = new Object();

	public AsynchronousTcpSession(int sessionId, long openTime, Config config, AsynchronousTcpWorker worker, AsynchronousSocketChannel socketChannel) {
		this.sessionId = sessionId;
		this.openTime = openTime;
		this.config = config;
		this.worker = worker;
		this.socketChannel = socketChannel;
		if (config.getReceiveByteBufferSize() > 0) {
			log.debug("fix buffer size: {}", config.getReceiveByteBufferSize());
			receiveBufferSizePredictor = new FixedReceiveBufferSizePredictor(config.getReceiveByteBufferSize());
		} else {
			log.debug("adaptive buffer size");
			receiveBufferSizePredictor = new AdaptiveReceiveBufferSizePredictor();
		}
		state = OPEN;
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
	
	private void write0(Object obj) {
		if(obj == null)
			return;
		
		synchronized(lock) {
			writeBuffer.offer(obj);
			if(!isWriting) {
				isWriting = true;
				worker.write(socketChannel, this, writeBuffer.poll());
			}
		}
	}

	@Override
	public void write(ByteBuffer byteBuffer) {
		write0(byteBuffer);
	}

	@Override
	public void write(FileRegion file) {
		try {
    		transferTo(file.getFile(), file.getPosition(), file.getCount());
    	} catch (Throwable t) {
			log.error("transfer file error", t);
		} finally {
    		file.releaseExternalResources();
    	}
	}
	
	public long transferTo(FileChannel fc, long pos, long len) throws Throwable {
		long ret = 0;
    	try {
	    	ByteBuffer buf = ByteBuffer.allocate(1024 * 4);
	    	int i = 0;
	    	while((i = fc.read(buf, pos)) != -1) {
	    		if(i > 0) {
	    			ret += i;
	    			pos += i;
	    			buf.flip();
	    			write0(buf);
	    			buf = ByteBuffer.allocate(1024 * 4);
	    		}
	    		// TODO test
	    		if(ret >= len)
	    			break;
	    	}
    	} finally {
    		fc.close();
    	}
    	return ret;
	}

	@Override
	public void close(boolean immediately) {
		if(immediately) {
			try {
				socketChannel.close();
			} catch (IOException e) {
				log.error("channel close error", e);
			}
			state = CLOSE;
			worker.eventManager.executeCloseTask(this);
		} else {
			write0(CLOSE_FLAG);
		}
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
		if (localAddress == null) {
			try {
				localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
			} catch (Throwable t) {
				log.error("get local address error", t);
			}
		}
		return localAddress;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		if (remoteAddress == null) {
			try {
				remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
			} catch (Throwable t) {
				log.error("get remote address error", t);
			}
		}
		return remoteAddress;
	}

}
