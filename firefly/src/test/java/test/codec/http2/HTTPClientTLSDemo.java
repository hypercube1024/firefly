package test.codec.http2;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTPClientConnection;
import com.firefly.client.http2.HTTPClientRequest;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.net.tcp.ssl.DefaultSSLContextFactory;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;

public class HTTPClientTLSDemo {

	public static void main(String[] args) throws InterruptedException, ExecutionException {
//		System.setProperty("javax.net.debug", "all");
		
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setSslContextFactory(new DefaultSSLContextFactory());
		http2Configuration.setSecureConnectionEnabled(true);
		http2Configuration.setTcpIdleTimeout(60 * 1000);
		HTTP2Client client = new HTTP2Client(http2Configuration);
		FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
		client.connect("www.baidu.com", 443, promise);

		final HTTPClientConnection httpConnection = promise.get();
		HTTPClientRequest request = new HTTPClientRequest("GET", "/");
		httpConnection.send(request, new ClientHTTPHandler.Adapter() {

			@Override
			public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
					HTTPConnection connection) {
				System.out.println(BufferUtils.toUTF8String(item));
				return false;
			}

			@Override
			public boolean messageComplete(Request request, Response response, HTTPOutputStream output,
					HTTPConnection connection) {
				System.out.println(response.getStatus() + "|" + response.getReason() + "|" + response.getFields());
				return true;
			}
		});

	}

}
