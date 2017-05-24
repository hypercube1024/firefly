package test.codec.http2;

import com.firefly.codec.http2.frame.*;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.Session;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Stream.Listener;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HTTP2ServerDemo {

	private static Logger log = LoggerFactory.getLogger("firefly-system");

	public static void main(String[] args) {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setSecureConnectionEnabled(true);
		http2Configuration.setFlowControlStrategy("simple");
		http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);

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
					public boolean onIdleTimeout(Stream stream, Throwable x) {
						log.info("idle timeout", x);
						return true;
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
		}, new ServerHTTPHandler.Adapter());

		server.start();
	}
}
