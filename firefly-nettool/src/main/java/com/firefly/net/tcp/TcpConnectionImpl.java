package com.firefly.net.tcp;

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

public class TcpConnectionImpl extends AbstractTcpConnection {

	public TcpConnectionImpl(Session session) {
		super(session);
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

}
