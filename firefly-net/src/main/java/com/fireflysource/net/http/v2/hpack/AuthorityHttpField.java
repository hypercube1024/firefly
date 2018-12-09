package com.fireflysource.net.http.v2.hpack;


import com.fireflysource.net.http.common.model.HostPortHttpField;
import com.fireflysource.net.http.common.model.HttpHeader;

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
