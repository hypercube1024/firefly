package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.codec.http2.stream.Session.Listener;;

abstract public class AbstractHTTP2Connection extends AbstractHTTPConnection {

	protected final HTTP2Session http2Session;
	protected final Parser parser;
	protected final Generator generator;

	protected static final Scheduler scheduler = Schedulers.createScheduler();
	
	public static void stopScheduler() {
		scheduler.stop();
	}

	public AbstractHTTP2Connection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession,
			Listener listener) {
		super(sslSession, tcpSession, HttpVersion.HTTP_2);

		FlowControlStrategy flowControl = null;
		switch (config.getFlowControlStrategy()) {
		case "buffer":
			flowControl = new BufferingFlowControlStrategy(config.getInitialStreamSendWindow(), 0.5f);
			break;
		case "simple":
			flowControl = new SimpleFlowControlStrategy(config.getInitialStreamSendWindow());
			break;
		default:
			flowControl = new SimpleFlowControlStrategy(config.getInitialStreamSendWindow());
			break;
		}
		this.generator = new Generator(config.getMaxDynamicTableSize(), config.getMaxHeaderBlockFragment());
		this.http2Session = initHTTP2Session(config, flowControl, listener);
		this.parser = initParser(config);
	}

	abstract protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
			Listener listener);

	abstract protected Parser initParser(HTTP2Configuration config);

	public com.firefly.codec.http2.stream.Session getHttp2Session() {
		return http2Session;
	}

}
