package test.http;

import java.io.PrintWriter;

import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleResponse;

public class ServerDemo3 {

	public static void main(String[] args) {
		SimpleHTTPServer server = new SimpleHTTPServer();
		server.headerComplete(request -> {
			request.messageComplete(req -> {
				SimpleResponse response = req.getResponse();
				String path = req.getRequest().getURI().getPath();

				response.getResponse().getFields().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());
				switch (path) {
				case "/index":
					response.getResponse().getFields();
					response.getResponse().getFields().put(HttpHeader.CONTENT_LENGTH, "11");
					try (PrintWriter writer = response.getPrintWriter()) {
						writer.print("hello index");
					}
					break;
				case "/testPost":
					System.out.println(req.getRequest().toString());
					System.out.println(req.getRequest().getFields());
					System.out.println(req.getStringBody());
					try (PrintWriter writer = response.getPrintWriter()) {
						writer.print("receive post -> " + req.getStringBody());
					}
					break;
				default:
					response.getResponse().setStatus(HttpStatus.NOT_FOUND_404);
					try (PrintWriter writer = response.getPrintWriter()) {
						writer.print("resource not found");
					}
					break;
				}
			});
		}).listen("localhost", 3322);
	}

}
