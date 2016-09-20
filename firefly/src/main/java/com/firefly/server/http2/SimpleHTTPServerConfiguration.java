package com.firefly.server.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;

public class SimpleHTTPServerConfiguration extends HTTP2Configuration {

	private String host;
	private int port;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
