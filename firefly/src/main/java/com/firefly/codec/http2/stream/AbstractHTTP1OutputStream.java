package com.firefly.codec.http2.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.net.Session;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

abstract public class AbstractHTTP1OutputStream extends OutputStream {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	protected final boolean clientMode;
	protected final MetaData info;
	protected boolean closed;
	protected boolean commited;

	public AbstractHTTP1OutputStream(MetaData info, boolean clientMode) {
		this.info = info;
		this.clientMode = clientMode;
	}

	public synchronized boolean isClosed() {
		return closed;
	}

	public synchronized boolean isCommited() {
		return commited;
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b });
	}

	@Override
	public void write(byte[] array, int offset, int length) throws IOException {
		write(ByteBuffer.wrap(array, offset, length));
	}

	public synchronized void writeAndClose(ByteBuffer data) throws IOException {
		if (closed)
			return;

		if (commited)
			return;

		try {
			final HttpGenerator generator = getHttpGenerator();
			final Session tcpSession = getSession();
			HttpGenerator.Result generatorResult;

			ByteBuffer header = getHeaderByteBuffer();

			generatorResult = generate(info, header, null, data, true);
			if (generatorResult == HttpGenerator.Result.FLUSH
					&& generator.getState() == HttpGenerator.State.COMPLETING) {
				tcpSession.encode(header);
				if (data != null) {
					tcpSession.encode(data);
				}
				generatorResult = generator.generateRequest(null, null, null, null, true);
				if (generatorResult == HttpGenerator.Result.DONE && generator.getState() == HttpGenerator.State.END) {
					generateHTTPMessageSuccessfully();
				} else {
					generateHTTPMessageExceptionally();
				}
			} else {
				generateHTTPMessageExceptionally();
			}
			commited = true;
		} finally {
			closed = true;
		}
	}

	public synchronized void write(ByteBuffer data) throws IOException {
		if (closed)
			return;

		final HttpGenerator generator = getHttpGenerator();
		final Session tcpSession = getSession();
		HttpGenerator.Result generatorResult;

		if (!commited) {
			ByteBuffer header = getHeaderByteBuffer();

			generatorResult = generate(info, header, null, data, false);
			if (generatorResult == HttpGenerator.Result.FLUSH
					&& generator.getState() == HttpGenerator.State.COMMITTED) {
				tcpSession.encode(header);
				tcpSession.encode(data);
			} else {
				generateHTTPMessageExceptionally();
			}
			commited = true;
		} else {
			if (generator.isChunking()) {
				ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);

				generatorResult = generate(null, null, chunk, data, false);
				if (generatorResult == HttpGenerator.Result.FLUSH
						&& generator.getState() == HttpGenerator.State.COMMITTED) {
					tcpSession.encode(chunk);
					tcpSession.encode(data);
				} else {
					generateHTTPMessageExceptionally();
				}
			} else {
				generatorResult = generate(null, null, null, data, false);
				if (generatorResult == HttpGenerator.Result.FLUSH
						&& generator.getState() == HttpGenerator.State.COMMITTED) {
					tcpSession.encode(data);
				} else {
					generateHTTPMessageExceptionally();
				}
			}
		}
	}

	@Override
	public synchronized void close() throws IOException {
		if (closed)
			return;

		try {
			log.debug("http1 output stream is closing");
			final HttpGenerator generator = getHttpGenerator();
			final Session tcpSession = getSession();
			HttpGenerator.Result generatorResult;

			if (!commited) {
				ByteBuffer header = getHeaderByteBuffer();
				generatorResult = generate(info, header, null, null, true);
				if (generatorResult == HttpGenerator.Result.FLUSH
						&& generator.getState() == HttpGenerator.State.COMPLETING) {
					tcpSession.encode(header);
					generatorResult = generate(null, null, null, null, true);
					if (generatorResult == HttpGenerator.Result.DONE
							&& generator.getState() == HttpGenerator.State.END) {
						generateHTTPMessageSuccessfully();
					} else {
						generateHTTPMessageExceptionally();
					}
				} else {
					generateHTTPMessageExceptionally();
				}
				commited = true;
			} else {
				if (generator.isChunking()) {
					log.debug("http1 output stream is generating chunk");
					ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
					generatorResult = generate(null, null, chunk, null, true);
					if (generatorResult == HttpGenerator.Result.CONTINUE
							&& generator.getState() == HttpGenerator.State.COMPLETING) {
						generatorResult = generate(null, null, chunk, null, true);
						if (generatorResult == HttpGenerator.Result.FLUSH
								&& generator.getState() == HttpGenerator.State.COMPLETING) {
							tcpSession.encode(chunk);

							generatorResult = generate(null, null, null, null, true);
							if (generatorResult == HttpGenerator.Result.DONE
									&& generator.getState() == HttpGenerator.State.END) {
								generateHTTPMessageSuccessfully();
							} else {
								generateHTTPMessageExceptionally();
							}
						} else {
							generateHTTPMessageExceptionally();
						}
					} else {
						generateHTTPMessageExceptionally();
					}
				} else {
					generatorResult = generate(null, null, null, null, true);
					if (generatorResult == HttpGenerator.Result.CONTINUE
							&& generator.getState() == HttpGenerator.State.COMPLETING) {
						generatorResult = generate(null, null, null, null, true);
						if (generatorResult == HttpGenerator.Result.DONE
								&& generator.getState() == HttpGenerator.State.END) {
							generateHTTPMessageSuccessfully();
						} else {
							generateHTTPMessageExceptionally();
						}
					} else {
						generateHTTPMessageExceptionally();
					}
				}
			}
		} finally {
			closed = true;
		}
	}

	protected HttpGenerator.Result generate(MetaData info, ByteBuffer header, ByteBuffer chunk, ByteBuffer content,
			boolean last) throws IOException {
		final HttpGenerator generator = getHttpGenerator();
		if (clientMode) {
			return generator.generateRequest((MetaData.Request) info, header, chunk, content, last);
		} else {
			return generator.generateResponse((MetaData.Response) info, header, chunk, content, last);
		}
	}

	abstract protected ByteBuffer getHeaderByteBuffer();

	abstract protected Session getSession();

	abstract protected HttpGenerator getHttpGenerator();

	abstract protected void generateHTTPMessageSuccessfully();

	abstract protected void generateHTTPMessageExceptionally();

}
