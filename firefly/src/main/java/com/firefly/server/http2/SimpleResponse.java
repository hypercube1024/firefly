package com.firefly.server.http2;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.codec.http2.model.CookieGenerator;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SimpleResponse implements Closeable {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	Response response;
	HTTPOutputStream output;
	PrintWriter printWriter;
	BufferedHTTPOutputStream bufferedOutputStream;
	int bufferSize = 8 * 1024;
	String characterEncoding = "UTF-8";
	boolean asynchronous;

	public SimpleResponse(Response response, HTTPOutputStream output) {
		this.output = output;
		this.response = response;
	}

	public Response getResponse() {
		return response;
	}

	public boolean isAsynchronous() {
		return asynchronous;
	}

	public void setAsynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
	}

	public void addCookie(Cookie cookie) {
		response.getFields().add(HttpHeader.SET_COOKIE, CookieGenerator.generateSetCookie(cookie));
	}

	public OutputStream getOutputStream() {
		if (printWriter != null) {
			throw new IllegalStateException("the response has used print writer");
		}

		if (bufferedOutputStream == null) {
			bufferedOutputStream = new BufferedHTTPOutputStream();
			return bufferedOutputStream;
		} else {
			return bufferedOutputStream;
		}
	}

	public PrintWriter getPrintWriter() {
		if (bufferedOutputStream != null) {
			throw new IllegalStateException("the response has used output stream");
		}
		if (printWriter == null) {
			try {
				printWriter = new PrintWriter(
						new OutputStreamWriter(new BufferedHTTPOutputStream(), characterEncoding));
			} catch (UnsupportedEncodingException e) {
				log.error("create print writer exception", e);
			}
			return printWriter;
		} else {
			return printWriter;
		}
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public boolean isClosed() {
		return output.isClosed();
	}

	public void close() throws IOException {
		if (bufferedOutputStream != null) {
			bufferedOutputStream.close();
		} else if (printWriter != null) {
			printWriter.close();
		}
	}

	public void flush() throws IOException {
		if (bufferedOutputStream != null) {
			bufferedOutputStream.flush();
		} else if (printWriter != null) {
			printWriter.flush();
		}
	}

	private class BufferedHTTPOutputStream extends OutputStream {

		private byte[] buf = new byte[bufferSize];
		private int count;

		@Override
		public synchronized void write(int b) throws IOException {
			if (count >= buf.length) {
				flush();
			}
			buf[count++] = (byte) b;
		}

		@Override
		public synchronized void write(byte[] array, int offset, int length) throws IOException {
			if (array == null || array.length == 0 || length <= 0) {
				return;
			}

			if (offset < 0) {
				throw new IllegalArgumentException("the offset is less than 0");
			}

			if (length >= buf.length) {
				flush();
				output.write(array, offset, length);
				return;
			}
			if (length > buf.length - count) {
				flush();
			}
			System.arraycopy(array, offset, buf, count, length);
			count += length;
		}

		@Override
		public synchronized void flush() throws IOException {
			if (count > 0) {
				output.write(buf, 0, count);
				count = 0;
				buf = new byte[bufferSize];
			}
		}

		@Override
		public synchronized void close() throws IOException {
			flush();
			output.close();
		}
	}
}
