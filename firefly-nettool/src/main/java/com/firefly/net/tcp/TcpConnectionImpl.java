package com.firefly.net.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.io.BufferUtils;

public class TcpConnectionImpl implements TcpConnection {

	Session session;
	Action0 closeCallback;
	Action1<ByteBuffer> buffer;
	Action1<Throwable> exception;
	volatile Object attachment;

	public TcpConnectionImpl(Session session) {
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
	public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}

			public void failed(Throwable x) {
				failed.call(x);
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}

			public void failed(Throwable x) {
				failed.call(x);
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}

			public void failed(Throwable x) {
				failed.call(x);
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(String message, Action0 succeeded, Action1<Throwable> failed) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, StandardCharsets.UTF_8);
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}

			public void failed(Throwable x) {
				failed.call(x);
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(String message, String charset, Action0 succeeded, Action1<Throwable> failed) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}

			public void failed(Throwable x) {
				failed.call(x);
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(FileRegion file, Action0 succeeded, Action1<Throwable> failed) {
		session.write(file, new Callback() {
			public void succeeded() {
				succeeded.call();
			}

			public void failed(Throwable x) {
				failed.call(x);
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded) {
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded) {
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded) {
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(String message, Action0 succeeded) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, StandardCharsets.UTF_8);
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(String message, String charset, Action0 succeeded) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
		session.write(byteBuffer, new Callback() {
			public void succeeded() {
				succeeded.call();
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(FileRegion file, Action0 succeeded) {
		session.write(file, new Callback() {
			public void succeeded() {
				succeeded.call();
			}
		});
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer byteBuffer) {
		session.write(byteBuffer, Callback.NOOP);
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer[] byteBuffer) {
		session.write(byteBuffer, Callback.NOOP);
		return this;
	}

	@Override
	public TcpConnection write(Collection<ByteBuffer> byteBuffer) {
		session.write(byteBuffer, Callback.NOOP);
		return this;
	}

	@Override
	public TcpConnection write(String message) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, StandardCharsets.UTF_8);
		session.write(byteBuffer, Callback.NOOP);
		return this;
	}

	@Override
	public TcpConnection write(String message, String charset) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
		session.write(byteBuffer, Callback.NOOP);
		return this;
	}

	@Override
	public TcpConnection write(FileRegion file) {
		session.write(file, Callback.NOOP);
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
	public TcpConnection closeCallback(Action0 closeCallback) {
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
