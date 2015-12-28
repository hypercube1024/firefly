package test.codec.http2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.server.http2.HTTP1ServerConnectionListener;
import com.firefly.server.http2.HTTP1ServerRequestHandler;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.HTTPServerRequest;
import com.firefly.server.http2.HTTPServerResponse;
import com.firefly.server.http2.HTTPServerResponse.HTTP1ServerResponseOutputStream;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.utils.io.BufferUtils;

public class HTTP1ServerDemo3 {

	public static void main(String[] args) {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		HTTP2Server server = new HTTP2Server("localhost", 6678, http2Configuration, new ServerSessionListener.Adapter(),
				new HTTP1ServerConnectionListener() {

					@Override
					public HTTP1ServerRequestHandler onNewConnectionIsCreating() {
						return new HTTP1ServerRequestHandler.Adapter() {
							@Override
							public boolean messageComplete(HTTPServerRequest request, HTTPServerResponse response) {
								HttpURI uri = request.getURI();
								System.out.println("current path is " + uri.getPath());
								System.out.println("current http headers are " + request.getFields());
								response.setStatus(200);
								
								try (HTTP1ServerResponseOutputStream output = response.getOutputStream()) {
									output.write(BufferUtils.toBuffer("hello the server demo\r\n", StandardCharsets.UTF_8));
									output.write(BufferUtils.toBuffer("test1\r\n", StandardCharsets.UTF_8));
								} catch (IOException e) {
									e.printStackTrace();
								}
								return true;
							}

						};
					}
				});
		server.start();
	}

}
