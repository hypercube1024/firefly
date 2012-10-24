package com.firefly.server.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;

public class NetBufferedOutputStream extends OutputStream {

	protected byte[] buf;
	protected int count;
	protected Session session;
	protected int bufferSize;
	protected boolean keepAlive;

	public NetBufferedOutputStream(Session session, int bufferSize,
			boolean keepAlive) {
		this.session = session;
		this.bufferSize = bufferSize;
		this.keepAlive = keepAlive;
		buf = new byte[bufferSize];
	}

	@Override
	public void write(int b) throws IOException {
		if (count >= buf.length)
			flush();

		buf[count++] = (byte) b;
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException {
		if (len >= buf.length) {
			flush();
			session.write(ByteBuffer.wrap(b, off, len));
			return;
		}
		if (len > buf.length - count)
			flush();

		System.arraycopy(b, off, buf, count, len);
		count += len;
	}

	public void write(File file, long off, long len) throws IOException {
		flush();
		FileRegion fileRegion = new FileRegion(new RandomAccessFile(file, "r"), off, len);
		session.write(fileRegion);
	}

	public void write(File file) throws IOException {
		write(file, 0, file.length());
	}

	@Override
	public void flush() throws IOException {
		if (count > 0) {
			session.write(ByteBuffer.wrap(buf, 0, count));
			resetBuffer();
		}
	}

	@Override
	public void close() throws IOException {
		flush();
		if (!keepAlive)
			session.close(false);
	}

	public void resetBuffer() {
		buf = new byte[bufferSize];
		count = 0;
	}

}
