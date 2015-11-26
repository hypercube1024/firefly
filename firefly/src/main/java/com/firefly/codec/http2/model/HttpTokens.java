package com.firefly.codec.http2.model;

/**
 * HTTP constants
 */
public interface HttpTokens {
	// Terminal symbols.
    static final byte COLON= (byte)':';
    static final byte TAB= 0x09;
    static final byte LINE_FEED= 0x0A;
    static final byte CARRIAGE_RETURN= 0x0D;
    static final byte SPACE= 0x20;
    static final byte[] CRLF = {CARRIAGE_RETURN,LINE_FEED};
    static final byte SEMI_COLON= (byte)';';

    public enum EndOfContent { UNKNOWN_CONTENT,NO_CONTENT,EOF_CONTENT,CONTENT_LENGTH,CHUNKED_CONTENT }
}
