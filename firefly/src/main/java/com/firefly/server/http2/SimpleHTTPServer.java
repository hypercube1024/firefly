package com.firefly.server.http2;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.codec.http2.model.CookieGenerator;
import com.firefly.codec.http2.model.CookieParser;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action3;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SimpleHTTPServer extends AbstractLifeCycle {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	protected HTTP2Server http2Server;
	protected SimpleHTTPServerConfiguration configuration;
	protected Action1<SimpleRequest> headerComplete;
	protected Action3<Integer, String, SimpleRequest> badMessage;
	protected Action1<SimpleRequest> earlyEof;
	protected Action1<HTTPConnection> acceptConnection;

	public SimpleHTTPServer() {
		this(new SimpleHTTPServerConfiguration());
	}

	public SimpleHTTPServer(SimpleHTTPServerConfiguration configuration) {
		this.configuration = configuration;
	}

	public SimpleHTTPServer headerComplete(Action1<SimpleRequest> headerComplete) {
		this.headerComplete = headerComplete;
		return this;
	}

	public SimpleHTTPServer earlyEof(Action1<SimpleRequest> earlyEof) {
		this.earlyEof = earlyEof;
		return this;
	}

	public SimpleHTTPServer badMessage(Action3<Integer, String, SimpleRequest> badMessage) {
		this.badMessage = badMessage;
		return this;
	}

	public SimpleHTTPServer acceptConnection(Action1<HTTPConnection> acceptConnection) {
		this.acceptConnection = acceptConnection;
		return this;
	}

	public class SimpleResponse extends MetaData.Response {

		HTTPOutputStream output;
		PrintWriter printWriter;
		BufferedHTTPOutputStream bufferedOutputStream;
		int bufferSize = 8 * 1024;
		String characterEncoding = "UTF-8";

		public SimpleResponse(Response response, HTTPOutputStream output) {
			super(response.getVersion(), response.getStatus(), response.getReason(), response.getFields(),
					response.getContentLength());
			this.output = output;
			this.setStatus(HttpStatus.OK_200);
		}

		public void addCookie(Cookie cookie) {
			getFields().add(HttpHeader.SET_COOKIE, CookieGenerator.generateSetCookie(cookie));
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

	public class SimpleRequest extends MetaData.Request {

		SimpleResponse response;

		HTTPConnection connection;

		Action1<ByteBuffer> content;
		Action1<SimpleRequest> messageComplete;
		List<ByteBuffer> requestBody = new ArrayList<>();

		List<Cookie> cookies;
		String stringBody;

		public SimpleRequest(Request request) {
			super(request);
		}

		public SimpleResponse getResponse() {
			return response;
		}

		public HTTPConnection getConnection() {
			return connection;
		}

		public List<ByteBuffer> getRequestBody() {
			return requestBody;
		}

		public SimpleRequest content(Action1<ByteBuffer> content) {
			this.content = content;
			return this;
		}

		public SimpleRequest messageComplete(Action1<SimpleRequest> messageComplete) {
			this.messageComplete = messageComplete;
			return this;
		}

		public String getStringBody(String charset) {
			if (stringBody == null) {
				stringBody = BufferUtils.toString(requestBody, charset);
				return stringBody;
			} else {
				return stringBody;
			}
		}

		public String getStringBody() {
			return getStringBody("UTF-8");
		}

		public <T> T getJsonBody(Class<T> clazz) {
			return Json.toObject(getStringBody(), clazz);
		}

		public JsonObject getJsonObjectBody() {
			return Json.toJsonObject(getStringBody());
		}

		public JsonArray getJsonArrayBody() {
			return Json.toJsonArray(getStringBody());
		}

		public List<Cookie> getCookies() {
			if (cookies == null) {
				String v = getFields().get(HttpHeader.COOKIE);
				cookies = CookieParser.parseCookie(v);
				return cookies;
			} else {
				return cookies;
			}
		}
	}

	@Override
	protected void init() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void destroy() {
		http2Server.stop();
	}

}
