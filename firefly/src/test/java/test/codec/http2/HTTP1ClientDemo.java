package test.codec.http2;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

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

public class HTTP1ClientDemo {
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setTcpIdleTimeout(60 * 1000);
		HTTP2Client client = new HTTP2Client(http2Configuration);
		FuturePromise<HTTPConnection> promise = new FuturePromise<>();
		client.connect("localhost", 6655, promise);
		HTTPConnection connection = promise.get();
		System.out.println(connection.getHttpVersion());
		if(connection.getHttpVersion() == HttpVersion.HTTP_1_1) {
			HTTP1ClientConnection http1ClientConnection = (HTTP1ClientConnection) connection;
			HTTPClientRequest request = new HTTPClientRequest("GET", "/index.html");
			
			http1ClientConnection.request(request, new HTTP1ClientResponseHandler(){

				@Override
				public void earlyEOF() {
				}

				@Override
				public boolean content(ByteBuffer item, HTTPClientResponse response, HTTP1ClientConnection connection) {
					System.out.println(BufferUtils.toString(item, StandardCharsets.UTF_8));
					return false;
				}

				@Override
				public boolean headerComplete(HTTPClientResponse response, HTTP1ClientConnection connection) {
					return false;
				}

				@Override
				public boolean messageComplete(HTTPClientResponse response, HTTP1ClientConnection connection) {
					System.out.println(response);
					System.out.println(response.getFields());
					return true;
				}

				@Override
				public void badMessage(int status, String reason, HTTPClientResponse response, HTTP1ClientConnection connection) {

					
				}});
		}
		
	}

}
