package com.firefly.client.http2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action3;
import com.firefly.utils.lang.pool.Pool;

public class SimpleHTTPClient {

	private static volatile SimpleHTTPClient client;

	private HTTP2Client http2Client;
	private Map<RequestBuilder, Pool<HTTPClientConnection>> pool = new ConcurrentHashMap<>();

	private SimpleHTTPClient(HTTP2Configuration http2Configuration) {
		http2Client = new HTTP2Client(http2Configuration);
	}

	public static SimpleHTTPClient create() {
		return create(new HTTP2Configuration());
	}

	public static SimpleHTTPClient create(HTTP2Configuration http2Configuration) {
		if (client != null) {
			return client;
		}

		synchronized (SimpleHTTPClient.class) {
			if (client != null) {
				return client;
			}

			client = new SimpleHTTPClient(http2Configuration);
			return client;
		}
	}

	public synchronized static void destroy() {
		if (client != null) {
			client.http2Client.stop();
			client = null;
		}
	}

	public class RequestBuilder {
		String host;
		int port;
		HttpVersion httpVersion;

		MetaData.Request request;
		List<ByteBuffer> requestBody = new ArrayList<>();
		Action1<Response> messageComplete;
		Action1<ByteBuffer> content;
		Action3<Integer, String, Response> badMessage;
		Action1<Response> earlyEof;

		public RequestBuilder request(MetaData.Request request) {
			this.request = request;
			return this;
		}

		public RequestBuilder write(ByteBuffer buffer) {
			requestBody.add(buffer);
			return this;
		}

		public RequestBuilder messageComplete(Action1<Response> messageComplete) {
			this.messageComplete = messageComplete;
			return this;
		}

		public RequestBuilder content(Action1<ByteBuffer> content) {
			this.content = content;
			return this;
		}

		public RequestBuilder badMessage(Action3<Integer, String, Response> badMessage) {
			this.badMessage = badMessage;
			return this;
		}

		public RequestBuilder earlyEof(Action1<Response> earlyEof) {
			this.earlyEof = earlyEof;
			return this;
		}

		public void end() {
			// TODO
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((host == null) ? 0 : host.hashCode());
			result = prime * result + ((httpVersion == null) ? 0 : httpVersion.hashCode());
			result = prime * result + port;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RequestBuilder other = (RequestBuilder) obj;
			if (host == null) {
				if (other.host != null)
					return false;
			} else if (!host.equals(other.host))
				return false;
			if (httpVersion != other.httpVersion)
				return false;
			if (port != other.port)
				return false;
			return true;
		}
	}

	public RequestBuilder connect(String host, int port) {
		RequestBuilder req = new RequestBuilder();
		req.host = host;
		req.port = port;
		return req;
	}

	private void request(RequestBuilder request) {
		// TODO
	}
}
