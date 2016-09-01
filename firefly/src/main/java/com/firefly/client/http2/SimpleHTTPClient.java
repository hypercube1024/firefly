package com.firefly.client.http2;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action3;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.json.Json;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.pool.BlockingPool;
import com.firefly.utils.lang.pool.BoundedBlockingPool;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SimpleHTTPClient extends AbstractLifeCycle {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final HTTP2Client http2Client;
	private final Map<RequestBuilder, BlockingPool<HTTPClientConnection>> poolMap = new ConcurrentHashMap<>();
	private final Scheduler scheduler;

	public SimpleHTTPClient() {
		this(new SimpleHTTPClientConfiguration());
	}

	public SimpleHTTPClient(SimpleHTTPClientConfiguration http2Configuration) {
		http2Client = new HTTP2Client(http2Configuration);
		scheduler = Schedulers.createScheduler();
		start();
	}

	public class RequestBuilder {
		String host;
		int port;

		MetaData.Request request;
		List<ByteBuffer> requestBody = new ArrayList<>();
		Action1<Response> messageComplete;
		Action1<ByteBuffer> content;
		Action3<Integer, String, Response> badMessage;
		Action1<Response> earlyEof;
		Promise<HTTPOutputStream> promise;

		public RequestBuilder put(String name, List<String> list) {
			request.getFields().put(name, list);
			return this;
		}

		public RequestBuilder put(HttpHeader header, String value) {
			request.getFields().put(header, value);
			return this;
		}

		public RequestBuilder put(String name, String value) {
			request.getFields().put(name, value);
			return this;
		}

		public RequestBuilder put(HttpField field) {
			request.getFields().put(field);
			return this;
		}

		public RequestBuilder addAll(HttpFields fields) {
			request.getFields().addAll(fields);
			return this;
		}

		public RequestBuilder add(HttpField field) {
			request.getFields().add(field);
			return this;
		}

		public RequestBuilder jsonBody(Object obj) {
			request.getFields().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON.asString());
			return body(Json.toJson(obj));
		}

		public RequestBuilder body(String content) {
			requestBody.add(BufferUtils.toBuffer(content, StandardCharsets.UTF_8));
			return this;
		}

		public RequestBuilder write(ByteBuffer buffer) {
			requestBody.add(buffer);
			return this;
		}

		public RequestBuilder output(Promise<HTTPOutputStream> promise) {
			this.promise = promise;
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
			send(this);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((host == null) ? 0 : host.hashCode());
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
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (host == null) {
				if (other.host != null)
					return false;
			} else if (!host.equals(other.host))
				return false;
			if (port != other.port)
				return false;
			return true;
		}

		private SimpleHTTPClient getOuterType() {
			return SimpleHTTPClient.this;
		}

	}

	public RequestBuilder get(String url) {
		return request(HttpMethod.GET.asString(), url);
	}

	public RequestBuilder post(String url) {
		return request(HttpMethod.POST.asString(), url);
	}

	public RequestBuilder head(String url) {
		return request(HttpMethod.HEAD.asString(), url);
	}

	public RequestBuilder put(String url) {
		return request(HttpMethod.PUT.asString(), url);
	}

	public RequestBuilder delete(String url) {
		return request(HttpMethod.DELETE.asString(), url);
	}

	public RequestBuilder request(HttpMethod method, String url) {
		return request(method.asString(), url);
	}

	public RequestBuilder request(String method, String url) {
		try {
			return request(method, new URL(url));
		} catch (MalformedURLException e) {
			log.error("url exception", e);
			throw new IllegalArgumentException(e);
		}

	}

	public RequestBuilder request(String method, URL url) {
		try {
			RequestBuilder req = new RequestBuilder();
			req.host = url.getHost();
			req.port = url.getPort() < 0 ? url.getDefaultPort() : url.getPort();
			req.request = new MetaData.Request(method, new HttpURI(url.toURI()), HttpVersion.HTTP_1_1,
					new HttpFields());
			return req;
		} catch (URISyntaxException e) {
			log.error("url exception", e);
			throw new IllegalArgumentException(e);
		}
	}

	private void send(RequestBuilder r) {
		SimpleHTTPClientConfiguration config = (SimpleHTTPClientConfiguration) http2Client.getHttp2Configuration();
		BlockingPool<HTTPClientConnection> pool = getPool(r);
		try {
			HTTPClientConnection connection = pool.take(config.getTakeConnectionTimeout(), TimeUnit.MILLISECONDS);

			ClientHTTPHandler handler = new ClientHTTPHandler.Adapter()
					.messageComplete((req, resp, outputStream, conn) -> {
						try {
							if (r.messageComplete != null) {
								r.messageComplete.call(resp);
							}
							return true;
						} finally {
							pool.release(connection);
						}
					}).content((buffer, req, resp, outputStream, conn) -> {
						if (r.content != null) {
							r.content.call(buffer);
						}
						return false;
					}).badMessage((errCode, reason, req, resp, outputStream, conn) -> {
						try {
							if (r.badMessage != null) {
								r.badMessage.call(errCode, reason, resp);
							}
						} finally {
							pool.release(connection);
						}
					}).earlyEOF((req, resp, outputStream, conn) -> {
						try {
							if (r.earlyEof != null) {
								r.earlyEof.call(resp);
							}
						} finally {
							pool.release(connection);
						}
					});

			if (r.requestBody != null && r.requestBody.isEmpty() == false) {
				connection.send(r.request, r.requestBody.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), handler);
			} else if (r.promise != null) {
				connection.send(r.request, r.promise, handler);
			} else {
				connection.send(r.request, handler);
			}
		} catch (InterruptedException e) {
			log.error("take connection exception", e);
		}
	}

	private BlockingPool<HTTPClientConnection> getPool(RequestBuilder request) {
		BlockingPool<HTTPClientConnection> pool = poolMap.get(request);
		if (pool == null) {
			synchronized (this) {
				if (pool == null) {
					SimpleHTTPClientConfiguration config = (SimpleHTTPClientConfiguration) http2Client
							.getHttp2Configuration();
					pool = new BoundedBlockingPool<>(config.getInitPoolSize(), config.getMaxPoolSize(), () -> {
						FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
						http2Client.connect(request.host, request.port, promise);
						try {
							return promise.get();
						} catch (InterruptedException | ExecutionException e) {
							log.error("create http connection exception", e);
							throw new IllegalStateException();
						}
					}, (conn) -> {
						return conn.isOpen();
					}, (conn) -> {
						try {
							conn.close();
						} catch (IOException e) {
							log.error("close http connection exception", e);
						}
					});
					poolMap.put(request, pool);
					return pool;
				}
			}
		}
		return pool;
	}

	@Override
	protected void init() {
		SimpleHTTPClientConfiguration config = (SimpleHTTPClientConfiguration) http2Client.getHttp2Configuration();
		scheduler.scheduleAtFixedRate(() -> {
			Iterator<Map.Entry<RequestBuilder, BlockingPool<HTTPClientConnection>>> iterator = poolMap.entrySet()
					.iterator();
			while (iterator.hasNext()) {
				Map.Entry<RequestBuilder, BlockingPool<HTTPClientConnection>> entry = iterator.next();
				entry.getValue().cleanup();
				if (log.isDebugEnabled()) {
					log.debug("clean up connection pool [{}:{}], current pool size is {}", entry.getKey().host,
							entry.getKey().port, entry.getValue().size());
				}
			}
		}, config.getCleanupInitialDelay(), config.getCleanupInterval(), TimeUnit.MILLISECONDS);
	}

	@Override
	protected void destroy() {
		http2Client.stop();
		scheduler.stop();
	}
}
