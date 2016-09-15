package com.firefly.server.http2;

import java.io.IOException;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action3;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SimpleHTTPServer extends AbstractLifeCycle {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	protected HTTP2Server http2Server;
	protected SimpleHTTPServerConfiguration configuration;
	protected Action1<SimpleRequest> headerComplete;
	protected Action3<Integer, String, SimpleRequest> badMessage;
	protected Action1<SimpleRequest> earlyEof;
	protected Action1<HTTPConnection> acceptConnection;

	public SimpleHTTPServer() {
		this(new SimpleHTTPServerConfiguration());
	}

	public SimpleHTTPServer(SimpleHTTPServerConfiguration configuration) {
		this.configuration = configuration;
	}

	public SimpleHTTPServer headerComplete(Action1<SimpleRequest> headerComplete) {
		this.headerComplete = headerComplete;
		return this;
	}

	public SimpleHTTPServer earlyEof(Action1<SimpleRequest> earlyEof) {
		this.earlyEof = earlyEof;
		return this;
	}

	public SimpleHTTPServer badMessage(Action3<Integer, String, SimpleRequest> badMessage) {
		this.badMessage = badMessage;
		return this;
	}

	public SimpleHTTPServer acceptConnection(Action1<HTTPConnection> acceptConnection) {
		this.acceptConnection = acceptConnection;
		return this;
	}

	public void listen(String host, int port) {
		configuration.setHost(host);
		configuration.setPort(port);
		listen();
	}

	public void listen() {
		start();
	}

	@Override
	protected void init() {
		http2Server = new HTTP2Server(configuration.getHost(), configuration.getPort(), configuration,
				new ServerHTTPHandler.Adapter().headerComplete((request, response, out, connection) -> {
					SimpleRequest r = new SimpleRequest(request, response, out);
					request.setAttachment(r);
					if (headerComplete != null) {
						headerComplete.call(r);
					}
					return false;
				}).content((buffer, request, response, out, connection) -> {
					SimpleRequest r = (SimpleRequest) request.getAttachment();
					if (r.content != null) {
						r.content.call(buffer);
					} else {
						r.requestBody.add(buffer);
					}
					return false;
				}).messageComplete((request, response, out, connection) -> {
					SimpleRequest r = (SimpleRequest) request.getAttachment();
					if (r.messageComplete != null) {
						r.messageComplete.call(r);
					}
					if (r.getResponse().isAsynchronous() == false) {
						if (r.getResponse().isClosed() == false) {
							try {
								r.getResponse().close();
							} catch (IOException e) {
								log.error("close response output stream exception", e);
							}
						}
					}
					return true;
				}).badMessage((status, reason, request, response, out, connection) -> {
					if (badMessage != null) {
						if (request.getAttachment() != null) {
							SimpleRequest r = (SimpleRequest) request.getAttachment();
							badMessage.call(status, reason, r);
						} else {
							SimpleRequest r = new SimpleRequest(request, response, out);
							request.setAttachment(r);
							badMessage.call(status, reason, r);
						}
					}
				}).earlyEOF((request, response, out, connection) -> {
					if (earlyEof != null) {
						if (request.getAttachment() != null) {
							SimpleRequest r = (SimpleRequest) request.getAttachment();
							earlyEof.call(r);
						} else {
							SimpleRequest r = new SimpleRequest(request, response, out);
							request.setAttachment(r);
							earlyEof.call(r);
						}
					}
				}));
		http2Server.start();
	}

	@Override
	protected void destroy() {
		http2Server.stop();
	}

}
