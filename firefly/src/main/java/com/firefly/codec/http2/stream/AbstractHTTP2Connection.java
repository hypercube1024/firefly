package com.firefly.codec.http2.stream;

import java.io.Closeable;
import java.io.IOException;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.codec.http2.stream.Session.Listener;;

public abstract class AbstractHTTP2Connection implements Closeable {
	
	public Object attachment;
	protected final SSLSession sslSession;
	protected final Session tcpSession;
	
	protected final HttpVersion httpVersion;
	protected HTTP2Session http2Session;
	protected Parser parser;
	protected Generator generator;
	
	protected volatile boolean closed;
	
	protected static final Scheduler scheduler = Schedulers.createScheduler();

	public AbstractHTTP2Connection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession, Listener listener, HttpVersion httpVersion) {
		this.sslSession = sslSession;
		this.tcpSession = tcpSession;
		this.httpVersion = httpVersion;
		
		switch (httpVersion) {
		case HTTP_2:
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
			break;
		case HTTP_1_1:
		case HTTP_1_0:
			// TODO create HTTP 1.1 parser and generator
			break;
		default:
			break;
		}
		
		tcpSession.attachObject(this);
	}
	
	abstract protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl, Listener listener);
	
	abstract protected Parser initParser(HTTP2Configuration config);

	
	public HttpVersion getHttpVersion() {
		return httpVersion;
	}

	public com.firefly.codec.http2.stream.Session getHttp2Session() {
		return http2Session;
	}

	public boolean isOpen() {
		return !closed;
	}
	
	public void close() throws IOException {
		if (sslSession != null && sslSession.isOpen()) {
			sslSession.close();
		}
		if(tcpSession != null && tcpSession.isOpen()) {
			tcpSession.close();
		}
		attachment = null;
		closed = true;
	}
	
}
