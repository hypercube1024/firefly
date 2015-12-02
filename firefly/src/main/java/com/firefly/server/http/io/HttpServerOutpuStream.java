package com.firefly.server.http.io;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import com.firefly.server.http.HttpServletRequestImpl;
import com.firefly.server.http.HttpServletResponseImpl;

public class HttpServerOutpuStream extends ServletOutputStream {
	protected Queue<ChunkedData> queue = new LinkedList<ChunkedData>();
	protected int size, bufferSize;
	protected NetBufferedOutputStream bufferedOutput;
	protected HttpServletResponseImpl response;
	protected HttpServletRequestImpl request;

	public HttpServerOutpuStream(int bufferSize,
			NetBufferedOutputStream bufferedOutput,
			HttpServletRequestImpl request, HttpServletResponseImpl response) {
		this.bufferSize = bufferSize;
		this.bufferedOutput = bufferedOutput;
		this.request = request;
		this.response = response;
	}

	@Override
	public void write(int b) throws IOException {
		queue.offer(new ByteChunkedData((byte) b));
		size++;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ChunkedData c = new ByteArrayChunkedData(b, off, len);
		queue.offer(c);
		size += len;
	}

	@Override
	public void print(String s) throws IOException {
		if (s == null)
			s = "null";
		if (s.length() > 0)
			write(response.stringToByte(s));
	}

	@Override
	public void flush() throws IOException {

	}

	@Override
	public void close() throws IOException {
		if (!response.isCommitted()) {
			response.setHeader("Content-Length", String.valueOf(size));
			byte[] head = response.getHeadData();
			bufferedOutput.write(head);
			response.setCommitted(true);
		}

		if (size > 0) {
			if (request.getMethod().equals("HEAD"))
				queue.clear();
			else {
				for (ChunkedData d = null; (d = queue.poll()) != null;)
					d.write();
			}

			size = 0;
		}
		bufferedOutput.close();
	}

	public void resetBuffer() {
		bufferedOutput.resetBuffer();
		size = 0;
		queue.clear();
	}

	private class ByteChunkedData extends ChunkedData {
		private byte b;

		public ByteChunkedData(byte b) {
			this.b = b;
		}

		@Override
		public void write() throws IOException {
			bufferedOutput.write(b);
		}
	}

	private class ByteArrayChunkedData extends ChunkedData {
		private byte[] b;
		private int len;

		public ByteArrayChunkedData(byte[] b, int off, int len) {
			this.b = new byte[len];
			System.arraycopy(b, off, this.b, 0, len);
			this.len = len;
		}

		@Override
		public void write() throws IOException {
			bufferedOutput.write(b, 0, len);
		}
		
		public String toString() {
			return b.toString();
		}
	}

	abstract protected class ChunkedData {
		abstract void write() throws IOException;
	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
		// TODO Auto-generated method stub
		
	}
}
