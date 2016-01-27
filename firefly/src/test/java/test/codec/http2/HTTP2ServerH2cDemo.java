package test.codec.http2;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.Session;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Stream.Listener;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;

public class HTTP2ServerH2cDemo {

	public static void main(String[] args) {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setFlowControlStrategy("simple");
		http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);

		final Map<Integer, Integer> settings = new HashMap<>();
		settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
		settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());

		HTTP2Server server = new HTTP2Server("127.0.0.1", 6677, http2Configuration,
				new ServerSessionListener.Adapter() {

					@Override
					public Map<Integer, Integer> onPreface(Session session) {
						System.out.println("session preface: " + session);
						return settings;
					}

					@Override
					public Listener onNewStream(Stream stream, HeadersFrame frame) {
						System.out.println("session new stream, " + frame.getMetaData() + "|" + stream);

						MetaData metaData = frame.getMetaData();
						if (metaData.isRequest()) {
							final MetaData.Request request = (MetaData.Request) metaData;
							HttpURI uri = request.getURI();
							if (uri.getPath().equals("/index")) {
								MetaData.Response response = new MetaData.Response(HttpVersion.HTTP_2, 200,
										new HttpFields());
								HeadersFrame responseFrame = new HeadersFrame(stream.getId(), response, null, true);
								stream.headers(responseFrame, new Callback() {

									@Override
									public void succeeded() {
										System.out.println("response success");
									}

									@Override
									public void failed(Throwable x) {
										x.printStackTrace();
									}
								});
								System.out.println("server response");
							} else if (uri.getPath().equals("/data")) {

							}
						}

						return new Listener.Adapter() {

							@Override
							public void onHeaders(Stream stream, HeadersFrame frame) {
								System.out.println("stream on headers " + frame.getMetaData() + "|" + stream);

							}

							@Override
							public void onData(Stream stream, DataFrame frame, Callback callback) {
								System.out.println("session on data, " + frame + "|"
										+ BufferUtils.toString(frame.getData(), StandardCharsets.UTF_8));
								if(frame.isEndStream()) {
									MetaData.Response response = new MetaData.Response(HttpVersion.HTTP_2, 200,
											new HttpFields());
									HeadersFrame responseFrame = new HeadersFrame(stream.getId(), response, null, true);
									stream.headers(responseFrame, new Callback() {

										@Override
										public void succeeded() {
											System.out.println("response data success");
										}

										@Override
										public void failed(Throwable x) {
											x.printStackTrace();
										}
									});
								}
								callback.succeeded();
							}
						};
					}

					@Override
					public void onAccept(Session session) {
						System.out.println("accept a new session " + session);

					}
				}, new ServerHTTPHandler.Adapter());
		server.start();
	}

}
