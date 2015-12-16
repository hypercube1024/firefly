package test.codec.http2;

import java.util.HashMap;
import java.util.Map;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.GoAwayFrame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PingFrame;
import com.firefly.codec.http2.frame.PushPromiseFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.Session;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Stream.Listener;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;

public class HTTP2ServerDemo {
	public static void main(String[] args) {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setFlowControlStrategy("simple");
		http2Configuration.setTcpIdleTimeout(10 * 60 * 1000);

		final Map<Integer, Integer> settings = new HashMap<>();
		settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
		settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());

		HTTP2Server server = new HTTP2Server("127.0.0.1", 6677, http2Configuration, new ServerSessionListener() {

			@Override
			public Map<Integer, Integer> onPreface(Session session) {
				System.out.println("server on preface: " + session);
				return settings;
			}

			@Override
			public Listener onNewStream(Stream stream, HeadersFrame frame) {
				System.out.println("server on new stream: " + stream.getId());
				System.out.println("server on new stream headers: " + frame.getMetaData().toString());

				return new Listener() {

					@Override
					public void onHeaders(Stream stream, HeadersFrame frame) {
						System.out.println("server on headers: " + frame.getMetaData());
					}

					@Override
					public Listener onPush(Stream stream, PushPromiseFrame frame) {
						return null;
					}

					@Override
					public void onData(Stream stream, DataFrame frame, Callback callback) {
						System.out.println("server data size:" + frame.remaining() + "|"
								+ BufferUtils.toUTF8String(frame.getData()));
						callback.succeeded();
					}

					@Override
					public void onReset(Stream stream, ResetFrame frame) {
						System.out.println("server reset: " + stream + "|" + frame);
					}

					@Override
					public void onTimeout(Stream stream, Throwable x) {
						x.printStackTrace();
					}
				};
			}

			@Override
			public void onSettings(Session session, SettingsFrame frame) {
				System.out.println("on settings: " + frame.toString());
			}

			@Override
			public void onPing(Session session, PingFrame frame) {
			}

			@Override
			public void onReset(Session session, ResetFrame frame) {
				System.out.println("server reset " + frame);
			}

			@Override
			public void onClose(Session session, GoAwayFrame frame) {
				System.out.println("server closed " + frame);
			}

			@Override
			public void onFailure(Session session, Throwable failure) {
				failure.printStackTrace();
			}

			@Override
			public void onAccept(Session session) {
			}
		});

		server.start();
	}
}
