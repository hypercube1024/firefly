package test.codec.http2;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Phaser;

import com.firefly.client.http2.HTTP1ClientConnection;
import com.firefly.client.http2.HTTP1ClientResponseHandler;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTPClientRequest;
import com.firefly.client.http2.HTTPClientResponse;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;

public class HTTP1ClientDemo2 {

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setTcpIdleTimeout(60 * 1000);
		HTTP2Client client = new HTTP2Client(http2Configuration);

		FuturePromise<HTTPConnection> promise = new FuturePromise<>();
		client.connect("localhost", 6655, promise);

		HTTPConnection connection = promise.get();
		System.out.println(connection.getHttpVersion());

		if (connection.getHttpVersion() == HttpVersion.HTTP_1_1) {
			HTTP1ClientConnection http1ClientConnection = (HTTP1ClientConnection) connection;

			final Phaser phaser = new Phaser(1);

			// request index.html
			HTTPClientRequest request = new HTTPClientRequest("GET", "/index");
			http1ClientConnection.request(request, new HTTP1ClientResponseHandler.Adapter() {

				@Override
				public boolean content(ByteBuffer item, HTTPClientResponse response, HTTP1ClientConnection connection) {
					System.out.println(BufferUtils.toString(item, StandardCharsets.UTF_8));
					return false;
				}

				@Override
				public boolean messageComplete(HTTPClientResponse response, HTTP1ClientConnection connection) {
					System.out.println(response);
					System.out.println(response.getFields());
					int currentPhaseNumber = phaser.arrive();
					System.out.println("current phase number: " + currentPhaseNumber);
					return true;
				}
			});

			phaser.awaitAdvance(0);
			System.out.println("demo2 request finished");
		}

	}

}
