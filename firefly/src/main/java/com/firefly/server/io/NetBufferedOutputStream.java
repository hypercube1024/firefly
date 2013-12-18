package com.firefly.server.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.server.http.HttpServletRequestImpl;
import com.firefly.server.http.HttpServletResponseImpl;
import com.firefly.server.http.Monitor;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class NetBufferedOutputStream extends OutputStream {

	private static Log access = LogFactory.getInstance().getLog("firefly-access");
	
	protected byte[] buf;
	protected int count;
	protected Session session;
	protected int bufferSize;
	protected boolean keepAlive;
	protected HttpServletRequestImpl request;
	protected HttpServletResponseImpl response;
	protected boolean hasSavedAccessLog = false;

	public NetBufferedOutputStream(Session session, HttpServletRequestImpl request, HttpServletResponseImpl response, int bufferSize,
			boolean keepAlive) {
		this.session = session;
		this.bufferSize = bufferSize;
		this.keepAlive = keepAlive;
		this.request = request;
		this.response = response;
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
			session.encode(ByteBuffer.wrap(b, off, len));
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
		session.encode(fileRegion);
	}

	public void write(File file) throws IOException {
		write(file, 0, file.length());
	}

	@Override
	public void flush() throws IOException {
		if (count > 0) {
			session.encode(ByteBuffer.wrap(buf, 0, count));
			resetBuffer();
		}
	}

	@Override
	public void close() throws IOException {
		try {
			flush();
			if (!keepAlive)
				session.close(false);
		} finally {
			if(!hasSavedAccessLog) {
				access.info("{}|{}|{}|{}|{}|{}|{}|{}|{}|{}", 
						session.getSessionId(), 
						Monitor.CONN_COUNT.get(),
						request.getHeader("X-Forwarded-For"),
						request.getRemoteAddr(),
						response.getStatus(),
						request.getProtocol(),
						request.getMethod(),
						request.getRequestURI(),
						request.getQueryString(),
						request.getTimeDifference());
				hasSavedAccessLog = true;
			}
		}
	}

	public void resetBuffer() {
		buf = new byte[bufferSize];
		count = 0;
	}

}
