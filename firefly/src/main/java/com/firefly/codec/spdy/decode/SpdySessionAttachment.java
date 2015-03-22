package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.net.tcp.ssl.SSLSession;

public class SpdySessionAttachment {
	public SSLSession sslSession;
	public ByteBuffer byteBuffer;
}
