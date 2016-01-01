package com.firefly.client.http2;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP1ClientResponseHandler implements ResponseHandler {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	protected HTTP1ClientConnection connection;
	protected MetaData.Response response;
	protected MetaData.Request request;
	protected HTTPOutputStream outputStream;
	protected final ClientHTTPHandler clientHTTPHandler;

	HTTP1ClientResponseHandler(ClientHTTPHandler clientHTTPHandler) {
		this.clientHTTPHandler = clientHTTPHandler;
	}

	@Override
	public final boolean startResponse(HttpVersion version, int status, String reason) {
		if (log.isDebugEnabled()) {
			log.debug("client received the response line, {}, {}, {}", version, status, reason);
		}

		if (status == 100 && "Continue".equalsIgnoreCase(reason)) {
			clientHTTPHandler.continueToSendData(request, response, outputStream, connection);
			if (log.isDebugEnabled()) {
				log.debug("client received 100 continue, current parser state is {}",
						connection.getParser().getState());
			}
			return true;
		} else {
			response = new HTTPClientResponse(version, status, reason);
			return false;
		}
	}

	@Override
	public final void parsedHeader(HttpField field) {
		response.getFields().add(field);
	}

	@Override
	public final int getHeaderCacheSize() {
		return 1024;
	}

	@Override
	public final boolean content(ByteBuffer item) {
		return clientHTTPHandler.content(item, request, response, outputStream, connection);
	}

	@Override
	public final boolean headerComplete() {
		return clientHTTPHandler.headerComplete(request, response, outputStream, connection);
	}

	protected boolean http1MessageComplete() {
		try {
			return clientHTTPHandler.messageComplete(request, response, outputStream, connection);
		} finally {
			String requestConnectionValue = request.getFields().get(HttpHeader.CONNECTION);
			String responseConnectionValue = response.getFields().get(HttpHeader.CONNECTION);

			connection.getParser().reset();

			switch (response.getVersion()) {
			case HTTP_1_0:
				if ("keep-alive".equalsIgnoreCase(requestConnectionValue)
						&& "keep-alive".equalsIgnoreCase(responseConnectionValue)) {
					log.debug("the client {} connection is persistent", response.getVersion());
				} else {
					try {
						connection.close();
					} catch (IOException e) {
						log.error("client closes connection exception", e);
					}
				}
				break;
			case HTTP_1_1: // the persistent connection is default in HTTP 1.1
				if ("close".equalsIgnoreCase(requestConnectionValue)
						|| "close".equalsIgnoreCase(responseConnectionValue)) {
					try {
						connection.close();
					} catch (IOException e) {
						log.error("client closes connection exception", e);
					}
				} else {
					log.debug("the client {} connection is persistent", response.getVersion());
				}
				break;
			default:
				throw new IllegalStateException(
						"client response does not support the http version " + connection.getHttpVersion());
			}

		}
	}

	@Override
	public final boolean messageComplete() {
		boolean success = connection.upgradeProtocolToHTTP2(request, response);
		if (success) {
			log.debug("client upgraded http2 successfully");
		}

		return http1MessageComplete();
	}

	@Override
	public final void badMessage(int status, String reason) {
		clientHTTPHandler.badMessage(status, reason, request, response, outputStream, connection);
	}

	@Override
	public void earlyEOF() {
		clientHTTPHandler.earlyEOF(request, response, outputStream, connection);
	}

}
