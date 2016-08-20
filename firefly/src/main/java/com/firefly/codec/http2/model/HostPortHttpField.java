package com.firefly.codec.http2.model;

import com.firefly.utils.StringUtils;

public class HostPortHttpField extends HttpField {
	private final String _host;
	private final int _port;

	public HostPortHttpField(String authority) {
		this(HttpHeader.HOST, HttpHeader.HOST.asString(), authority);
	}

	public HostPortHttpField(HttpHeader header, String name, String authority) {
		super(header, name, authority);
		if (authority == null || authority.length() == 0) {
			_host = "";
			_port = 0;
			return;
		}
		try {
			if (authority.charAt(0) == '[') {
				// ipv6reference
				int close = authority.lastIndexOf(']');
				if (close < 0)
					throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Bad ipv6");
				_host = authority.substring(0, close + 1);

				if (authority.length() > close + 1) {
					if (authority.charAt(close + 1) != ':')
						throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Bad ipv6 port");
					_port = StringUtils.toInt(authority, close + 2);
				} else
					_port = 0;
			} else {
				// ipv4address or hostname
				int c = authority.lastIndexOf(':');
				if (c >= 0) {
					_host = authority.substring(0, c);
					_port = StringUtils.toInt(authority, c + 1);
				} else {
					_host = authority;
					_port = 0;
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
		return _host;
	}

	/**
	 * Get the port.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return _port;
	}
}
