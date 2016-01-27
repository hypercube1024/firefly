package test.codec.http2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTPClientConnection;
import com.firefly.client.http2.HTTPClientRequest;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.HostPortHttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpScheme;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ClientH2cDemo2 {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static void main(String[] args)
			throws InterruptedException, ExecutionException, UnsupportedEncodingException {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setFlowControlStrategy("simple");
		http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);
		HTTP2Client client = new HTTP2Client(http2Configuration);

		FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
		client.connect("127.0.0.1", 6677, promise);

		final HTTPClientConnection httpConnection = promise.get();
		HTTPClientRequest request = new HTTPClientRequest("GET", "/index");

		Map<Integer, Integer> settings = new HashMap<>();
		settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
		settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
		SettingsFrame settingsFrame = new SettingsFrame(settings, false);

		final ByteBuffer[] buffers = new ByteBuffer[] { ByteBuffer.wrap("hello world!".getBytes("UTF-8")),
				ByteBuffer.wrap("big hello world!".getBytes("UTF-8")) };
		FuturePromise<HTTPClientConnection> http2Promise = new FuturePromise<>();
		ClientHTTPHandler handler = new ClientHTTPHandler.Adapter() {
			@Override
			public void continueToSendData(Request request, Response response, HTTPOutputStream output,
					HTTPConnection connection) {
				log.info("client received 100 continue");
				try (HTTPOutputStream out = output) {
					for (ByteBuffer buf : buffers) {
						out.write(buf);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
					HTTPConnection connection) {
				log.info("client received data: {}", BufferUtils.toUTF8String(item));
				return false;
			}

			@Override
			public boolean messageComplete(Request request, Response response, HTTPOutputStream output,
					HTTPConnection connection) {
				log.info("client received frame: {}, {}, {}", response.getStatus(), response.getReason(),
						response.getFields());
				return true;
			}
		};

		httpConnection.upgradeHTTP2(request, settingsFrame, http2Promise, handler);

		HTTPClientConnection clientConnection = http2Promise.get();

		HttpFields fields = new HttpFields();
		fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");
		MetaData.Request post = new MetaData.Request("POST", HttpScheme.HTTP, new HostPortHttpField("127.0.0.1:6677"),
				"/data", HttpVersion.HTTP_1_1, fields);
		clientConnection.sendRequestWithContinuation(post, handler);

		MetaData.Request get = new MetaData.Request("GET", HttpScheme.HTTP, new HostPortHttpField("127.0.0.1:6677"),
				"/test2", HttpVersion.HTTP_1_1, new HttpFields());
		clientConnection.send(get, handler);

		MetaData.Request post2 = new MetaData.Request("POST", HttpScheme.HTTP, new HostPortHttpField("127.0.0.1:6677"),
				"/data", HttpVersion.HTTP_1_1, fields);
		clientConnection.send(post2, new ByteBuffer[] { ByteBuffer.wrap("test data 2".getBytes("UTF-8")),
				ByteBuffer.wrap("finished test data 2".getBytes("UTF-8")) }, handler);
	}

}
