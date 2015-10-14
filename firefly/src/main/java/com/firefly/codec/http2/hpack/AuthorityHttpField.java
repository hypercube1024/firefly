package com.firefly.codec.http2.hpack;

import com.firefly.codec.http2.model.HostPortHttpField;
import com.firefly.codec.http2.model.HttpHeader;

public class AuthorityHttpField extends HostPortHttpField {
	public final static String AUTHORITY = HpackContext.STATIC_TABLE[1][0];

	public AuthorityHttpField(String authority) {
		super(HttpHeader.C_AUTHORITY, AUTHORITY, authority);
	}

	@Override
	public String toString() {
		return String.format("%s(preparsed h=%s p=%d)", super.toString(), getHost(), getPort());
	}
}
