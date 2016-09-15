package com.firefly.net.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;

public class TcpConnectionImpl implements TcpConnection {

	Session session;
	Action0 closeCallback;
	Action1<Throwable> exception;

	public TcpConnectionImpl(Session session) {
		this.session = session;
	}

	@Override
	public TcpConnection receive(Action1<ByteBuffer> buffer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection exception(Action1<Throwable> exception) {
		this.exception = exception;
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(String message, Action0 succeeded, Action1<Throwable> failed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(FileRegion file, Action0 succeeded, Action1<Throwable> failed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(String message, Action0 succeeded) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(FileRegion file, Action0 succeeded) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(ByteBuffer byteBuffer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(ByteBuffer[] byteBuffer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(Collection<ByteBuffer> byteBuffer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(String message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TcpConnection write(FileRegion file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttachment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttachment(Object object) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getOpenTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getCloseTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDuration() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLastReadTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLastWrittenTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLastActiveTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getReadBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getWrittenBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public TcpConnection closeCallback(Action0 closeCallback) {
		this.closeCallback = closeCallback;
		return this;
	}

	@Override
	public void close() {

	}

	@Override
	public void closeNow() {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdownOutput() {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdownInput() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getState() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getIdleTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

}
