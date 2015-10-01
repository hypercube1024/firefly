package com.firefly.codec.http2.model;

import com.firefly.utils.StringUtils;

/**
 */
public class HostPortHttpField extends HttpField {
	private final String host;
	private final int port;

	public HostPortHttpField(String authority) {
		this(HttpHeader.HOST, HttpHeader.HOST.asString(), authority);
	}

	public HostPortHttpField(HttpHeader header, String name, String authority) {
		super(header, name, authority);
		if (authority == null || authority.length() == 0)
			throw new IllegalArgumentException("No Authority");
		try {
			if (authority.charAt(0) == '[') {
				// ipv6reference
				int close = authority.lastIndexOf(']');
				if (close < 0)
					throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Bad ipv6");
				host = authority.substring(0, close + 1);

				if (authority.length() > close + 1) {
					if (authority.charAt(close + 1) != ':')
						throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Bad ipv6 port");
					port = StringUtils.toInt(authority, close + 2);
				} else
					port = 0;
			} else {
				// ipv4address or hostname
				int c = authority.lastIndexOf(':');
				if (c >= 0) {
					host = authority.substring(0, c);
					port = StringUtils.toInt(authority, c + 1);
				} else {
					host = authority;
					port = 0;
				}
			}
		} catch (BadMessageException bm) {
			throw bm;
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
		return host;
	}

	/**
	 * Get the port.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
}
