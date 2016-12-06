package test.http;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.utils.io.BufferUtils;

public class ServerDemo4 {

	public static void main(String[] args) {
		SimpleHTTPServer server = new SimpleHTTPServer();
		server.headerComplete(req -> {
			List<ByteBuffer> list = new ArrayList<>();
			req.content(list::add).messageComplete(request -> {
				SimpleResponse response = req.getResponse();
				String path = req.getRequest().getURI().getPath();

				response.getResponse().getFields().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());

				switch (path) {
				case "/":
					try (PrintWriter writer = response.getPrintWriter()) {
						writer.print("server demo 4");
					}
					break;
				case "/postData":
					System.out.println(req.getRequest().toString());
					System.out.println(req.getRequest().getFields());
					String msg = BufferUtils.toString(list, "UTF-8");
					System.out.println(msg);
					try (PrintWriter writer = response.getPrintWriter()) {
						writer.print("receive message -> " + msg);
					}
					break;

				default:
					response.getResponse().setStatus(HttpStatus.NOT_FOUND_404);
					try (PrintWriter writer = response.getPrintWriter()) {
						writer.print("resource not found");
					}
					break;
				}

				System.out.println(request.getRequest().toString());
				System.out.println(request.getRequest().getFields());
				String msg = BufferUtils.toString(list, "UTF-8");
				System.out.println(msg);
			});

		}).listen("localhost", 3333);
	}

}
