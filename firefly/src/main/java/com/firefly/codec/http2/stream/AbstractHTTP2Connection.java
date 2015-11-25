package com.firefly.codec.http2.stream;

import java.io.Closeable;
import java.io.IOException;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.codec.http2.stream.Session.Listener;;

public abstract class AbstractHTTP2Connection implements Closeable {
	
	public Object attachment;
	protected final SSLSession sslSession;
	protected final com.firefly.net.Session tcpSession;
	protected final Parser parser;
	protected final Generator generator;
	
	protected static final Scheduler scheduler = Schedulers.createScheduler();

	public AbstractHTTP2Connection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession, Listener listener) {
		this.sslSession = sslSession;
		this.tcpSession = tcpSession;
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
		this.parser = initParser(config, flowControl, listener);
		tcpSession.attachObject(this);
	}
	
	abstract protected Parser initParser(HTTP2Configuration config, FlowControlStrategy flowControl, Listener listener);

	public Parser getParser() {
		return parser;
	}

	public Generator getGenerator() {
		return generator;
	}
	
	public void close() throws IOException {
		if (sslSession != null) {
			sslSession.close();
		}
		attachment = null;
	}
	
}
