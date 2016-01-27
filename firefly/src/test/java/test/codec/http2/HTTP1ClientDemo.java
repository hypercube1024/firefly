package test.codec.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Phaser;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.client.http2.HTTP1ClientConnection;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTPClientConnection;
import com.firefly.client.http2.HTTPClientRequest;
import com.firefly.codec.http2.model.Cookie;
import com.firefly.codec.http2.model.CookieGenerator;
import com.firefly.codec.http2.model.CookieParser;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;

public class HTTP1ClientDemo {

	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);
		HTTP2Client client = new HTTP2Client(http2Configuration);

		FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
		client.connect("localhost", 6655, promise);

		HTTPConnection connection = promise.get();
		System.out.println(connection.getHttpVersion());

		if (connection.getHttpVersion() == HttpVersion.HTTP_1_1) {
			HTTP1ClientConnection http1ClientConnection = (HTTP1ClientConnection) connection;

			final Phaser phaser = new Phaser(2);

			// request index.html
			HTTPClientRequest request = new HTTPClientRequest("GET", "/index.html");
			http1ClientConnection.send(request, new ClientHTTPHandler.Adapter() {

				@Override
				public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
						HTTPConnection connection) {
					System.out.println(BufferUtils.toString(item, StandardCharsets.UTF_8));
					return false;
				}

				@Override
				public boolean messageComplete(Request request, Response response, HTTPOutputStream output,
						HTTPConnection connection) {
					System.out.println(response);
					System.out.println(response.getFields());
					int currentPhaseNumber = phaser.arrive();
					System.out.println("current phase number: " + currentPhaseNumber);
					return true;
				}

			});
			phaser.arriveAndAwaitAdvance();

			final List<Cookie> currentCookies = new CopyOnWriteArrayList<>();
			// login
			HTTPClientRequest loginRequest = new HTTPClientRequest("GET", "/login");
			http1ClientConnection.send(loginRequest, new ClientHTTPHandler.Adapter() {

				@Override
				public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
						HTTPConnection connection) {
					System.out.println(BufferUtils.toString(item, StandardCharsets.UTF_8));
					return false;
				}

				@Override
				public boolean messageComplete(Request request, Response response, HTTPOutputStream output,
						HTTPConnection connection) {
					System.out.println(response);
					System.out.println(response.getFields());
					String cookieString = response.getFields().get(HttpHeader.SET_COOKIE);
					if (VerifyUtils.isNotEmpty(cookieString)) {
						Cookie cookie = CookieParser.parseSetCookie(cookieString);
						currentCookies.add(cookie);
					}

					int currentPhaseNumber = phaser.arrive();
					System.out.println("current phase number: " + currentPhaseNumber);
					return true;
				}
			});
			phaser.arriveAndAwaitAdvance();

			System.out.println("current cookies : " + currentCookies);
			// post data
			HTTPClientRequest post = new HTTPClientRequest("POST", "/add");
			post.getFields().add(new HttpField(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded"));

			for (Cookie cookie : currentCookies) {
				if (cookie.getName().equals("jsessionid")) {
					post.getFields().add(new HttpField(HttpHeader.COOKIE, CookieGenerator.generateCookie(cookie)));
				}
			}

			ByteBuffer data = ByteBuffer.wrap("content=hello_world".getBytes(StandardCharsets.UTF_8));
			ByteBuffer data2 = ByteBuffer.wrap("_data2test".getBytes(StandardCharsets.UTF_8));
			ByteBuffer[] dataArray = new ByteBuffer[] { data, data2 };

			http1ClientConnection.send(post, dataArray, new ClientHTTPHandler.Adapter() {

				@Override
				public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
						HTTPConnection connection) {
					System.out.println(BufferUtils.toString(item, StandardCharsets.UTF_8));
					return false;
				}

				@Override
				public boolean messageComplete(Request request, Response response, HTTPOutputStream output,
						HTTPConnection connection) {
					System.out.println(response);
					System.out.println(response.getFields());
					int currentPhaseNumber = phaser.arrive();
					System.out.println("current phase number: " + currentPhaseNumber);
					return true;
				}
			});
			phaser.arriveAndAwaitAdvance();

			// post single data
			HTTPClientRequest postSingleData = new HTTPClientRequest("POST", "/add");
			postSingleData.getFields().add(new HttpField(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded"));

			for (Cookie cookie : currentCookies) {
				if (cookie.getName().equals("jsessionid")) {
					postSingleData.getFields()
							.add(new HttpField(HttpHeader.COOKIE, CookieGenerator.generateCookie(cookie)));
				}
			}

			ByteBuffer data1 = ByteBuffer.wrap("content=test_post_single_data".getBytes(StandardCharsets.UTF_8));
			http1ClientConnection.send(post, data1, new ClientHTTPHandler.Adapter() {

				@Override
				public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
						HTTPConnection connection) {
					System.out.println(BufferUtils.toString(item, StandardCharsets.UTF_8));
					return false;
				}

				@Override
				public boolean messageComplete(Request request, Response response, HTTPOutputStream output,
						HTTPConnection connection) {
					System.out.println(response);
					System.out.println(response.getFields());
					int currentPhaseNumber = phaser.arrive();
					System.out.println("current phase number: " + currentPhaseNumber);
					return true;
				}
			});
			phaser.arriveAndAwaitAdvance();

			System.out.println("request finished");
			http1ClientConnection.close();
		} else {

		}

	}

}
