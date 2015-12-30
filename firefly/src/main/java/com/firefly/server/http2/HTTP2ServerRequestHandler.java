package com.firefly.server.http2;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.AbstractHTTP2OutputStream;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Stream.Listener;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ServerRequestHandler extends ServerSessionListener.Adapter {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	private final ServerHTTPHandler serverHTTPHandler;
	HTTP2ServerConnection connection;

	public HTTP2ServerRequestHandler(ServerHTTPHandler serverHTTPHandler) {
		this.serverHTTPHandler = serverHTTPHandler;
	}

	@Override
	public Listener onNewStream(final Stream stream, HeadersFrame headersFrame) {
		if (!headersFrame.getMetaData().isRequest()) {
			throw new IllegalArgumentException(
					"the stream " + stream.getId() + " received meta data that is not request type");
		}

		final MetaData.Request request = (MetaData.Request) headersFrame.getMetaData();
		final MetaData.Response response = new HTTPServerResponse();
		final AbstractHTTP2OutputStream output = new AbstractHTTP2OutputStream(response, false) {

			@Override
			protected Stream getStream() {
				return stream;
			}
		};
		stream.setAttribute("outputStream", output);
		serverHTTPHandler.headerComplete(request, response, output, connection);

		if (headersFrame.isEndStream()) {
			serverHTTPHandler.messageComplete(request, response, output, connection);
		}

		return new Listener.Adapter() {

			@Override
			public void onData(Stream stream, DataFrame dataFrame, Callback callback) {
				try {
					serverHTTPHandler.content(dataFrame.getData(), request, response, output, connection);
					callback.succeeded();
				} catch (Throwable t) {
					callback.failed(t);
				}

				if (dataFrame.isEndStream()) {
					serverHTTPHandler.messageComplete(request, response, output, connection);
				}
			}

			@Override
			public void onReset(Stream stream, ResetFrame frame) {
				ErrorCode errorCode = ErrorCode.from(frame.getError());
				String reason = errorCode == null ? "error=" + frame.getError() : errorCode.name().toLowerCase();
				serverHTTPHandler.badMessage(frame.getError(), reason, request, response, output, connection);
			}

		};
	}

}
