package com.firefly.client.http2;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.stream.BufferingFlowControlStrategy;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.SimpleFlowControlStrategy;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;

public class HTTP2ClientSessionAttachment {

	public SSLSession sslSession;
	public Object attachment;
	private final Parser parser;
	private final Generator generator;
	private final com.firefly.net.Session endPoint;

	private static final Scheduler scheduler = Schedulers.createScheduler();

	public HTTP2ClientSessionAttachment(HTTP2ClientConfiguration config, Parser parser,
			com.firefly.net.Session endPoint, Listener listener) {
		this.endPoint = endPoint;
		FlowControlStrategy flowControl = null;
		switch (config.getFlowControlStrategy()) {
		case "buffer":
			flowControl = new BufferingFlowControlStrategy(config.getInitialStreamSendWindow(), 0.5f);
			break;
		case "simple":
			flowControl = new SimpleFlowControlStrategy(config.getInitialStreamSendWindow());
			break;
		default:
			break;
		}
		this.generator = new Generator(config.getMaxDynamicTableSize(), config.getMaxHeaderBlockFragment());
		HTTP2ClientSession http2ClientSession = new HTTP2ClientSession(scheduler, endPoint, this.generator, listener,
				flowControl, config.getStreamIdleTimeout());
		this.parser = new Parser(http2ClientSession, config.getMaxDynamicTableSize(), config.getMaxRequestHeadLength());
	}

	public Parser getParser() {
		return parser;
	}

	public Generator getGenerator() {
		return generator;
	}

	public com.firefly.net.Session getEndPoint() {
		return endPoint;
	}

}
