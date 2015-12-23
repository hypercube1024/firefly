package com.firefly.client.http2;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.frame.PrefaceFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.codec.http2.stream.AbstractHTTP2Connection;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTP2Session;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.codec.http2.stream.SessionSPI;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ClientConnection extends AbstractHTTP2Connection {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public void initialize(HTTP2Configuration config, final Promise<HTTPConnection> promise, final Listener listener) {
		Map<Integer, Integer> settings = listener.onPreface(getHttp2Session());
		if (settings == null) {
			settings = Collections.emptyMap();
		}
		PrefaceFrame prefaceFrame = new PrefaceFrame();
		SettingsFrame settingsFrame = new SettingsFrame(settings, false);
		SessionSPI sessionSPI = getSessionSPI();
		int windowDelta = config.getInitialSessionRecvWindow() - FlowControlStrategy.DEFAULT_WINDOW_SIZE;
		Callback callback = new Callback() {

			@Override
			public void succeeded() {
				promise.succeeded(HTTP2ClientConnection.this);
			}

			@Override
			public void failed(Throwable x) {
				try {
					HTTP2ClientConnection.this.close();
				} catch (IOException e) {
					log.error("http2 client connection initialization error", e);
				}
				promise.failed(x);
			}
		};

		if (windowDelta > 0) {
			sessionSPI.updateRecvWindow(windowDelta);
			sessionSPI.frames(null, callback, prefaceFrame, settingsFrame, new WindowUpdateFrame(0, windowDelta));
		} else {
			sessionSPI.frames(null, callback, prefaceFrame, settingsFrame);
		}
	}

	public HTTP2ClientConnection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession,
			Listener listener) {
		super(config, tcpSession, sslSession, listener);
	}

	protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
			Listener listener) {
		HTTP2ClientSession http2ClientSession = new HTTP2ClientSession(scheduler, this.tcpSession, this.generator,
				listener, flowControl, config.getStreamIdleTimeout());
		return http2ClientSession;
	}

	protected Parser initParser(HTTP2Configuration config) {
		return new Parser((HTTP2ClientSession) http2Session, config.getMaxDynamicTableSize(),
				config.getMaxRequestHeadLength());
	}

	Parser getParser() {
		return parser;
	}

	Generator getGenerator() {
		return generator;
	}

	SSLSession getSSLSession() {
		return sslSession;
	}

	SessionSPI getSessionSPI() {
		return http2Session;
	}

}
