package com.firefly.codec.http2.model;

public class HostPortHttpField extends HttpField {
	final HostPort _hostPort;

	public HostPortHttpField(String authority) {
		this(HttpHeader.HOST, HttpHeader.HOST.asString(), authority);
	}

	protected HostPortHttpField(HttpHeader header, String name, String authority) {
		super(header, name, authority);
		try {
			_hostPort = new HostPort(authority);
		} catch (Exception e) {
			throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Bad HostPort", e);
		}
	}

	/**
	 * Get the host.
	 * 
	 * @return the host
	 */
	public String getHost() {
		return _hostPort.getHost();
	}

	/**
	 * Get the port.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return _hostPort.getPort();
	}

	/**
	 * Get the port.
	 * 
	 * @param defaultPort
	 * @return the port
	 */
	public int getPort(int defaultPort) {
		return _hostPort.getPort(defaultPort);
	}
}
