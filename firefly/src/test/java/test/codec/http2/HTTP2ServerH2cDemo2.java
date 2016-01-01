package test.codec.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ServerH2cDemo2 {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static void main(String[] args) {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setFlowControlStrategy("simple");
		http2Configuration.setTcpIdleTimeout(60 * 1000);

		final Map<Integer, Integer> settings = new HashMap<>();
		settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
		settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());

		HTTP2Server server = new HTTP2Server("127.0.0.1", 6677, http2Configuration, new ServerHTTPHandler.Adapter() {

			@Override
			public boolean accept100Continue(Request request, Response response, HTTPOutputStream output,
					HTTPConnection connection) {
				log.info("received expect continue ");
				return false;
			}

			@Override
			public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
					HTTPConnection connection) {
				log.info("received data, {}", BufferUtils.toString(item, StandardCharsets.UTF_8));
				return false;
			}

			@Override
			public boolean messageComplete(Request request, Response response, HTTPOutputStream outputStream,
					HTTPConnection connection) {
				log.info("received end frame, {}, {}", request.getURI(), request.getFields());
				HttpURI uri = request.getURI();
				if (uri.getPath().equals("/index")) {
					response.setStatus(HttpStatus.Code.OK.getCode());
					response.setReason(HttpStatus.Code.OK.getMessage());
					try (HTTPOutputStream output = outputStream) {
						output.writeWithContentLength(
								BufferUtils.toBuffer("receive initial stream successful", StandardCharsets.UTF_8));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (uri.getPath().equals("/data")) {
					response.setStatus(HttpStatus.Code.OK.getCode());
					response.setReason(HttpStatus.Code.OK.getMessage());
					try (HTTPOutputStream output = outputStream) {
						output.write(BufferUtils.toBuffer("receive data stream successful", StandardCharsets.UTF_8));
						output.write(BufferUtils.toBuffer("thank you", StandardCharsets.UTF_8), true);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					response.setStatus(HttpStatus.Code.NOT_FOUND.getCode());
					response.setReason(HttpStatus.Code.NOT_FOUND.getMessage());
					try (HTTPOutputStream output = outputStream) {
						output.writeWithContentLength(
								BufferUtils.toBuffer(uri.getPath() + " not found", StandardCharsets.UTF_8));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return true;
			}
		});
		server.start();
	}

}
