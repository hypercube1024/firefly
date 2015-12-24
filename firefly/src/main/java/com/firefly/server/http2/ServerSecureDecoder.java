package com.firefly.server.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.DecoderChain;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ServerSecureDecoder extends DecoderChain {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public ServerSecureDecoder(DecoderChain next) {
		super(next);
	}

	@Override
	public void decode(ByteBuffer buf, Session session) throws Throwable {
		if (session.getAttachment() instanceof HTTPConnection) {
			HTTPConnection connection = (HTTPConnection) session.getAttachment();

			ByteBuffer plaintext;
			switch (connection.getHttpVersion()) {
			case HTTP_2:
				plaintext = ((HTTP2ServerConnection) connection).getSSLSession().read(buf);
				break;
			case HTTP_1_1:
				plaintext = ((HTTP1ServerConnection) connection).getSSLSession().read(buf);
				break;
			default:
				throw new IllegalStateException(
						"server does not support the http version " + connection.getHttpVersion());
			}

			if (plaintext != null && next != null)
				next.decode(plaintext, session);
		} else if (session.getAttachment() instanceof HTTP2ServerSSLHandshakeContext) {
			HTTP2ServerSSLHandshakeContext context = (HTTP2ServerSSLHandshakeContext) session.getAttachment();
			SSLSession sslSession = context.sslSession;
			ByteBuffer plaintext = sslSession.read(buf);

			if (plaintext != null && plaintext.hasRemaining()) {
				log.debug("server session {} handshake finished and received cleartext size {}", session.getSessionId(),
						plaintext.remaining());
				if (session.getAttachment() instanceof HTTPConnection) {
					if (next != null) {
						next.decode(plaintext, session);
					}
				} else {
					throw new IllegalStateException("the server http connection has not been created");
				}
			} else {
				log.debug("server ssl session {} is shaking hands", session.getSessionId());
			}
		}
	}

}
