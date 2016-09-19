package com.firefly.net.tcp;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.io.BufferUtils;

public class SecureTcpConnectionImpl extends AbstractTcpConnection {

	SSLSession sslSession;

	public SecureTcpConnectionImpl(Session session, SSLSession sslSession) {
		super(session);
		this.sslSession = sslSession;
	}

	@Override
	public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
		try {
			sslSession.write(byteBuffer, new Callback() {
				public void succeeded() {
					succeeded.call();
				}

				public void failed(Throwable x) {
					failed.call(x);
				}
			});
		} catch (Throwable e) {
			failed.call(e);
		}
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
		try {
			sslSession.write(byteBuffer, new Callback() {
				public void succeeded() {
					succeeded.call();
				}

				public void failed(Throwable x) {
					failed.call(x);
				}
			});
		} catch (Throwable e) {
			failed.call(e);
		}
		return this;
	}

	@Override
	public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
		try {
			sslSession.write(byteBuffer.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), new Callback() {
				public void succeeded() {
					succeeded.call();
				}

				public void failed(Throwable x) {
					failed.call(x);
				}
			});
		} catch (Throwable e) {
			failed.call(e);
		}
		return this;
	}

	@Override
	public TcpConnection write(String message, Action0 succeeded, Action1<Throwable> failed) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, StandardCharsets.UTF_8);
		try {
			sslSession.write(byteBuffer, new Callback() {
				public void succeeded() {
					succeeded.call();
				}

				public void failed(Throwable x) {
					failed.call(x);
				}
			});
		} catch (Throwable e) {
			failed.call(e);
		}
		return this;
	}

	@Override
	public TcpConnection write(String message, String charset, Action0 succeeded, Action1<Throwable> failed) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
		try {
			sslSession.write(byteBuffer, new Callback() {
				public void succeeded() {
					succeeded.call();
				}

				public void failed(Throwable x) {
					failed.call(x);
				}
			});
		} catch (Throwable e) {
			failed.call(e);
		}
		return this;
	}

	@Override
	public TcpConnection write(FileRegion file, Action0 succeeded, Action1<Throwable> failed) {
		try {
			sslSession.transferFileRegion(file, new Callback() {
				public void succeeded() {
					succeeded.call();
				}

				public void failed(Throwable x) {
					failed.call(x);
				}
			});
		} catch (Throwable e) {
			failed.call(e);
		}
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded) {
		try {
			sslSession.write(byteBuffer, new Callback() {
				public void succeeded() {
					succeeded.call();
				}
			});
		} catch (Throwable e) {

		}
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded) {
		try {
			sslSession.write(byteBuffer, new Callback() {
				public void succeeded() {
					succeeded.call();
				}
			});
		} catch (Throwable e) {

		}
		return this;
	}

	@Override
	public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded) {
		try {
			sslSession.write(byteBuffer.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), new Callback() {
				public void succeeded() {
					succeeded.call();
				}
			});
		} catch (Throwable e) {
		}
		return this;
	}

	@Override
	public TcpConnection write(String message, Action0 succeeded) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, StandardCharsets.UTF_8);
		try {
			sslSession.write(byteBuffer, new Callback() {
				public void succeeded() {
					succeeded.call();
				}
			});
		} catch (Throwable e) {
		}
		return this;
	}

	@Override
	public TcpConnection write(String message, String charset, Action0 succeeded) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
		try {
			sslSession.write(byteBuffer, new Callback() {
				public void succeeded() {
					succeeded.call();
				}
			});
		} catch (Throwable e) {
		}
		return this;
	}

	@Override
	public TcpConnection write(FileRegion file, Action0 succeeded) {
		try {
			sslSession.transferFileRegion(file, new Callback() {
				public void succeeded() {
					succeeded.call();
				}
			});
		} catch (Throwable e) {
		}
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer byteBuffer) {
		try {
			sslSession.write(byteBuffer, Callback.NOOP);
		} catch (Throwable e) {
		}
		return this;
	}

	@Override
	public TcpConnection write(ByteBuffer[] byteBuffer) {
		try {
			sslSession.write(byteBuffer, Callback.NOOP);
		} catch (Throwable e) {
		}
		return this;
	}

	@Override
	public TcpConnection write(Collection<ByteBuffer> byteBuffer) {
		try {
			sslSession.write(byteBuffer.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), Callback.NOOP);
		} catch (Throwable e) {
		}
		return this;
	}

	@Override
	public TcpConnection write(String message) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, StandardCharsets.UTF_8);
		try {
			sslSession.write(byteBuffer, Callback.NOOP);
		} catch (Throwable e) {
		}
		return this;
	}

	@Override
	public TcpConnection write(String message, String charset) {
		ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
		try {
			sslSession.write(byteBuffer, Callback.NOOP);
		} catch (Throwable e) {
		}
		return this;
	}

	@Override
	public TcpConnection write(FileRegion file) {
		try {
			sslSession.transferFileRegion(file, Callback.NOOP);
		} catch (Throwable e) {
		}
		return this;
	}

}
