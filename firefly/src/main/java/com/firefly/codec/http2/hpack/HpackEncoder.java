package com.firefly.codec.http2.hpack;

import java.nio.ByteBuffer;
import java.util.EnumSet;

import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.PreEncodedHttpField;

public class HpackEncoder {
	private final static HttpField[] status = new HttpField[599];

	final static EnumSet<HttpHeader> DO_NOT_HUFFMAN = EnumSet.of(
			HttpHeader.AUTHORIZATION, 
			HttpHeader.CONTENT_MD5,
			HttpHeader.PROXY_AUTHENTICATE, 
			HttpHeader.PROXY_AUTHORIZATION);

	final static EnumSet<HttpHeader> DO_NOT_INDEX = EnumSet.of(
			// HttpHeader.C_PATH, // TODO more data needed
			// HttpHeader.DATE, // TODO more data needed
			HttpHeader.AUTHORIZATION, 
			HttpHeader.CONTENT_MD5, 
			HttpHeader.CONTENT_RANGE, 
			HttpHeader.ETAG,
			HttpHeader.IF_MODIFIED_SINCE, 
			HttpHeader.IF_UNMODIFIED_SINCE, 
			HttpHeader.IF_NONE_MATCH, 
			HttpHeader.IF_RANGE,
			HttpHeader.IF_MATCH, 
			HttpHeader.LOCATION, 
			HttpHeader.RANGE, 
			HttpHeader.RETRY_AFTER,
			// HttpHeader.EXPIRES,
			HttpHeader.LAST_MODIFIED, 
			HttpHeader.SET_COOKIE, 
			HttpHeader.SET_COOKIE2);

	final static EnumSet<HttpHeader> NEVER_INDEX = EnumSet.of(
			HttpHeader.AUTHORIZATION,
			HttpHeader.SET_COOKIE,
			HttpHeader.SET_COOKIE2);
	
	static {
        for (HttpStatus.Code code : HttpStatus.Code.values())
            status[code.getCode()] = new PreEncodedHttpField(HttpHeader.C_STATUS,Integer.toString(code.getCode()));
    }

	static void encodeValue(ByteBuffer buffer, boolean huffman, String value) {
		if (huffman) {
			// huffman literal value
			buffer.put((byte) 0x80);
			NBitInteger.encode(buffer, 7, Huffman.octetsNeeded(value));
			Huffman.encode(buffer, value);
		} else {
			// add literal assuming iso_8859_1
			buffer.put((byte) 0x00);
			NBitInteger.encode(buffer, 7, value.length());
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c < ' ' || c > 127)
					throw new IllegalArgumentException();
				buffer.put((byte) c);
			}
		}
	}

}
