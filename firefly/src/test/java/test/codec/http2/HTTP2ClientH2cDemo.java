package test.codec.http2;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.client.http2.HTTP1ClientConnection;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTP2ClientConnection;
import com.firefly.client.http2.HTTPClientRequest;
import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.HostPortHttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpScheme;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.Session;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ClientH2cDemo {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static void main(String[] args)
			throws InterruptedException, ExecutionException, UnsupportedEncodingException {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setFlowControlStrategy("simple");
		http2Configuration.setTcpIdleTimeout(60 * 1000);
		HTTP2Client client = new HTTP2Client(http2Configuration);

		FuturePromise<HTTPConnection> promise = new FuturePromise<>();
		client.connect("127.0.0.1", 6677, promise);

		HTTPConnection connection = promise.get();
		if (connection.getHttpVersion() == HttpVersion.HTTP_1_1) {
			final HTTP1ClientConnection httpConnection = (HTTP1ClientConnection) connection;
			HTTPClientRequest request = new HTTPClientRequest("GET", "/index");

			Map<Integer, Integer> settings = new HashMap<>();
			settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
			settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
			SettingsFrame settingsFrame = new SettingsFrame(settings, false);

			FuturePromise<HTTPConnection> http2promise = new FuturePromise<>();
			FuturePromise<Stream> initStream = new FuturePromise<>();
			httpConnection.upgradeHTTP2WithCleartext(request, settingsFrame, http2promise, initStream,
					new Stream.Listener.Adapter() {
						@Override
						public void onHeaders(Stream stream, HeadersFrame frame) {
							log.info("client stream {} received init headers: {}", stream.getId(), frame.getMetaData());
						}

					}, new Listener.Adapter() {

						@Override
						public Map<Integer, Integer> onPreface(Session session) {
							log.info("client preface: {}", session);
							Map<Integer, Integer> settings = new HashMap<>();
							settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
							settings.put(SettingsFrame.INITIAL_WINDOW_SIZE,
									http2Configuration.getInitialStreamSendWindow());
							return settings;
						}

						@Override
						public void onFailure(Session session, Throwable failure) {
							log.error("client failure, {}", failure, session);
						}
					}, new ClientHTTPHandler.Adapter());

			HTTPConnection connection2 = http2promise.get();
			if (connection2.getHttpVersion() == HttpVersion.HTTP_2) {

				HTTP2ClientConnection clientConnection = (HTTP2ClientConnection) connection2;

				HttpFields fields = new HttpFields();
				fields.put(HttpHeader.ACCEPT, "text/html");
				fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");
				fields.put(HttpHeader.CONTENT_LENGTH, "28");
				MetaData.Request metaData = new MetaData.Request("POST", HttpScheme.HTTP,
						new HostPortHttpField("127.0.0.1:6677"), "/data", HttpVersion.HTTP_2, fields);

				FuturePromise<Stream> streamPromise = new FuturePromise<>();
				clientConnection.getHttp2Session().newStream(new HeadersFrame(metaData, null, false), streamPromise,
						new Stream.Listener.Adapter() {

							@Override
							public void onHeaders(Stream stream, HeadersFrame frame) {
								log.info("client received headers: {}", frame.getMetaData());
							}

							@Override
							public void onData(Stream stream, DataFrame frame, Callback callback) {
								log.info("client received data: {}, {}", BufferUtils.toUTF8String(frame.getData()),
										frame);
								callback.succeeded();
							}
						});

				final Stream clientStream = streamPromise.get();
				log.info("client stream id is {}", clientStream.getId());

				final DataFrame smallDataFrame = new DataFrame(clientStream.getId(),
						ByteBuffer.wrap("hello world!".getBytes("UTF-8")), false);
				final DataFrame bigDataFrame = new DataFrame(clientStream.getId(),
						ByteBuffer.wrap("big hello world!".getBytes("UTF-8")), true);

				clientStream.data(smallDataFrame, new Callback() {

					@Override
					public void succeeded() {
						log.info("client sents small data success");
						clientStream.data(bigDataFrame, new Callback() {

							@Override
							public void succeeded() {
								log.info("client sents big data success");

							}

							@Override
							public void failed(Throwable x) {
								log.info("client sents big data failure");
							}
						});
					}

					@Override
					public void failed(Throwable x) {
						log.info("client sents small data failure");
					}
				});

			}
		}
	}

}
