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
import com.firefly.server.http2.HTTP1ServerConnectionListener;
import com.firefly.server.http2.HTTP1ServerRequestHandler;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ServerDemo {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static void main(String[] args) {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setSecure(true);
		http2Configuration.setFlowControlStrategy("simple");
		http2Configuration.setTcpIdleTimeout(60 * 1000);

		final Map<Integer, Integer> settings = new HashMap<>();
		settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
		settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());

		HTTP2Server server = new HTTP2Server("127.0.0.1", 6677, http2Configuration, new ServerSessionListener() {

			@Override
			public Map<Integer, Integer> onPreface(Session session) {
				log.info("server received preface: {}", session);
				return settings;
			}

			@Override
			public Listener onNewStream(Stream stream, HeadersFrame frame) {
				log.info("server created new stream: {}", stream.getId());
				log.info("server created new stream headers: {}", frame.getMetaData().toString());

				return new Listener() {

					@Override
					public void onHeaders(Stream stream, HeadersFrame frame) {
						log.info("server received headers: {}", frame.getMetaData());
					}

					@Override
					public Listener onPush(Stream stream, PushPromiseFrame frame) {
						return null;
					}

					@Override
					public void onData(Stream stream, DataFrame frame, Callback callback) {
						log.info("server received data {}, {}", BufferUtils.toUTF8String(frame.getData()), frame);
						callback.succeeded();
					}

					@Override
					public void onReset(Stream stream, ResetFrame frame) {
						log.info("server reseted: {} | {}", stream, frame);
					}

					@Override
					public void onTimeout(Stream stream, Throwable x) {
						log.error("the server stream {}, is timeout", x, stream);
					}
				};
			}

			@Override
			public void onSettings(Session session, SettingsFrame frame) {
				log.info("server received settings: {}", frame);
			}

			@Override
			public void onPing(Session session, PingFrame frame) {
			}

			@Override
			public void onReset(Session session, ResetFrame frame) {
				log.info("server reset " + frame);
			}

			@Override
			public void onClose(Session session, GoAwayFrame frame) {
				log.info("server closed " + frame);
			}

			@Override
			public void onFailure(Session session, Throwable failure) {
				log.error("server failure, {}", failure, session);
			}

			@Override
			public void onAccept(Session session) {
			}

			@Override
			public boolean onIdleTimeout(Session session) {
				return false;
			}
		}, new HTTP1ServerConnectionListener(){

			@Override
			public HTTP1ServerRequestHandler onNewConnectionIsCreating() {
				return new HTTP1ServerRequestHandler.Adapter();
			}});
		
		server.start();
	}
}
