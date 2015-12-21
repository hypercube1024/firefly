package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

abstract public class AbstractHTTP1Connection extends AbstractHTTPConnection {

	protected HttpParser parser;
	protected HttpGenerator generator;

	public AbstractHTTP1Connection(HTTP2Configuration config, SSLSession sslSession, Session tcpSession,
			RequestHandler requestHandler, ResponseHandler responseHandler) {
		super(sslSession, tcpSession, HttpVersion.HTTP_1_1);

		initHttpParser(config, requestHandler, responseHandler);
		generator = new HttpGenerator();
	}

	abstract protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler,
			ResponseHandler responseHandler);

}
