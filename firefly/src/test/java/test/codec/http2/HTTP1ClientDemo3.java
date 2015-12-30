package test.codec.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Phaser;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.client.http2.HTTP1ClientConnection;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTPClientRequest;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;

public class HTTP1ClientDemo3 {

	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setTcpIdleTimeout(60 * 1000);
		HTTP2Client client = new HTTP2Client(http2Configuration);

		FuturePromise<HTTPConnection> promise = new FuturePromise<>();
		client.connect("localhost", 6678, promise);

		HTTPConnection connection = promise.get();
		System.out.println(connection.getHttpVersion());

		if (connection.getHttpVersion() == HttpVersion.HTTP_1_1) {
			HTTP1ClientConnection http1ClientConnection = (HTTP1ClientConnection) connection;

			final Phaser phaser = new Phaser(2);

			// request index.html
			HTTPClientRequest request = new HTTPClientRequest("GET", "/index?version=1&test=ok");
			http1ClientConnection.request(request, new ClientHTTPHandler.Adapter() {

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

			// test 100-continue
			HTTPClientRequest post = new HTTPClientRequest("POST", "/testContinue");
			final ByteBuffer data = BufferUtils.toBuffer("client test continue 100 ", StandardCharsets.UTF_8);
			post.getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(data.remaining()));

			http1ClientConnection.requestWith100Continue(post, new ClientHTTPHandler.Adapter() {
				@Override
				public void continueToSendData(Request request, Response response, HTTPOutputStream output,
						HTTPConnection connection) {
					try {
						output.write(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

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

			System.out.println("demo2 request finished");
			http1ClientConnection.close();
		}
	}

}
