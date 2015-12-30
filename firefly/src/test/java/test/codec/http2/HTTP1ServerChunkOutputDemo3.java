package test.codec.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.server.http2.HTTP1ServerConnection;
import com.firefly.server.http2.HTTP1ServerConnection.HTTP1ServerResponseOutputStream;
import com.firefly.server.http2.HTTP1ServerConnectionListener;
import com.firefly.server.http2.HTTP1ServerRequestHandler;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.HTTPServerRequest;
import com.firefly.server.http2.HTTPServerResponse;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.utils.io.BufferUtils;

public class HTTP1ServerChunkOutputDemo3 {

	public static void main(String[] args) {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setTcpIdleTimeout(10 * 60 * 1000);

		HTTP2Server server = new HTTP2Server("localhost", 6678, http2Configuration,
				new ServerSessionListener.Adapter(), new HTTP1ServerConnectionListener() {

					@Override
					public HTTP1ServerRequestHandler onNewConnectionIsCreating() {
						return new HTTP1ServerRequestHandler.Adapter() {

							@Override
							public void earlyEOF(HTTPServerRequest request, HTTPServerResponse response,
									HTTP1ServerConnection connection) {
								System.out.println(
										"the server connection " + connection.getSessionId() + " is early EOF");
							}

							@Override
							public void badMessage(int status, String reason, HTTPServerRequest request,
									HTTPServerResponse response, HTTP1ServerConnection connection) {
								System.out.println("the server received a bad message, " + status + "|" + reason);

								try {
									connection.close();
								} catch (IOException e) {
									e.printStackTrace();
								}

							}

							@Override
							public boolean messageComplete(HTTPServerRequest request, HTTPServerResponse response,
									HTTP1ServerConnection connection) {
								HttpURI uri = request.getURI();
								System.out.println("current path is " + uri.getPath());
								System.out.println("current http headers are " + request.getFields());
								response.setStatus(200);

								List<ByteBuffer> list = new ArrayList<>();
								list.add(BufferUtils.toBuffer("hello the server demo ", StandardCharsets.UTF_8));
								list.add(BufferUtils.toBuffer("test chunk 1 ", StandardCharsets.UTF_8));
								list.add(BufferUtils.toBuffer("test chunk 2 ", StandardCharsets.UTF_8));
								list.add(BufferUtils.toBuffer("中文的内容，哈哈 ", StandardCharsets.UTF_8));
								list.add(BufferUtils.toBuffer("靠！！！ ", StandardCharsets.UTF_8));

								try (HTTP1ServerResponseOutputStream output = connection.getOutputStream()) {
									for (ByteBuffer buffer : list) {
										output.write(buffer);
									}
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
