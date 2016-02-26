package com.firefly.codec.http2.model;

/**
 * HTTP compliance modes:
 * <dl>
 * <dt>RFC7230</dt>
 * <dd>(default) Compliance with RFC7230</dd>
 * <dt>RFC2616</dt>
 * <dd>Wrapped/Continued headers and HTTP/0.9 supported</dd>
 * <dt>LEGACY</dt>
 * <dd>(aka STRICT) Adherence to Servlet Specification requirement for exact
 * case of header names, bypassing the header caches, which are case
 * insensitive, otherwise equivalent to RFC2616</dd>
 * </dl>
 */
public enum HttpCompliance {
	LEGACY, RFC2616, RFC7230
}