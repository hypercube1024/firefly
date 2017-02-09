package com.firefly.net.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.firefly.net.Session;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;

public abstract class AbstractTcpConnection implements TcpConnection {

	Session session;
	Action0 closeCallback;
	Action1<ByteBuffer> buffer;
	Action1<Throwable> exception;
	volatile Object attachment;
	
	public AbstractTcpConnection(Session session) {
		this.session = session;
	}

	@Override
	public TcpConnection receive(Action1<ByteBuffer> buffer) {
		this.buffer = buffer;
		return this;
	}

	@Override
	public TcpConnection exception(Action1<Throwable> exception) {
		this.exception = exception;
		return this;
	}

	@Override
	public Object getAttachment() {
		return attachment;
	}

	@Override
	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	@Override
	public int getSessionId() {
		return session.getSessionId();
	}

	@Override
	public long getOpenTime() {
		return session.getOpenTime();
	}

	@Override
	public long getCloseTime() {
		return session.getCloseTime();
	}

	@Override
	public long getDuration() {
		return session.getDuration();
	}

	@Override
	public long getLastReadTime() {
		return session.getLastReadTime();
	}

	@Override
	public long getLastWrittenTime() {
		return session.getLastWrittenTime();
	}

	@Override
	public long getLastActiveTime() {
		return session.getLastActiveTime();
	}

	@Override
	public long getReadBytes() {
		return session.getReadBytes();
	}

	@Override
	public long getWrittenBytes() {
		return session.getWrittenBytes();
	}

	@Override
	public TcpConnection close(Action0 closeCallback) {
		this.closeCallback = closeCallback;
		return this;
	}

	@Override
	public void close() {
		session.close();
	}

	@Override
	public void closeNow() {
		session.closeNow();
	}

	@Override
	public void shutdownOutput() {
		session.shutdownOutput();
	}

	@Override
	public void shutdownInput() {
		session.shutdownInput();
	}

	@Override
	public int getState() {
		return session.getState();
	}

	@Override
	public boolean isOpen() {
		return session.isOpen();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return session.getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return session.getRemoteAddress();
	}

	@Override
	public long getIdleTimeout() {
		return session.getIdleTimeout();
	}

}
