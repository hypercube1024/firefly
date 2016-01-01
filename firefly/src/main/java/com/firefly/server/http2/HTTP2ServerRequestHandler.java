package com.firefly.server.http2;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.AbstractHTTP2OutputStream;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Stream.Listener;
import com.firefly.utils.VerifyUtils;
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
	public Listener onNewStream(final Stream stream, final HeadersFrame headersFrame) {
		if (!headersFrame.getMetaData().isRequest()) {
			throw new IllegalArgumentException(
					"the stream " + stream.getId() + " received meta data that is not request type");
		}

		if (log.isDebugEnabled()) {
			log.debug("the remote stream {} is created, the header is {}", stream.getId(), headersFrame.getMetaData());
		}

		final MetaData.Request request = (MetaData.Request) headersFrame.getMetaData();
		final MetaData.Response response = new HTTPServerResponse();
		final AbstractHTTP2OutputStream output = new AbstractHTTP2OutputStream(response, false) {

			@Override
			protected Stream getStream() {
				return stream;
			}
		};

		String expectedValue = request.getFields().get(HttpHeader.EXPECT);
		if ("100-continue".equalsIgnoreCase(expectedValue)) {
			boolean skipNext = serverHTTPHandler.accept100Continue(request, response, output, connection);
			if (!skipNext) {
				MetaData.Response continue100 = new MetaData.Response(HttpVersion.HTTP_1_1, 100, "Continue",
						new HttpFields(), Long.MIN_VALUE);
				output.writeFrame(new HeadersFrame(stream.getId(), continue100, null, false));
				serverHTTPHandler.headerComplete(request, response, output, connection);
			}
		} else {
			serverHTTPHandler.headerComplete(request, response, output, connection);

			if (headersFrame.isEndStream()) {
				serverHTTPHandler.messageComplete(request, response, output, connection);
			}
		}

		return new Listener.Adapter() {

			@Override
			public void onHeaders(Stream stream, HeadersFrame endHeaderframe) {
				if (log.isDebugEnabled()) {
					log.debug("the stream {} received the end frame {}", stream.getId(), endHeaderframe);
				}
				if (endHeaderframe.isEndStream()) {
					String trailerName = request.getFields().get(HttpHeader.TRAILER);
					if (VerifyUtils.isNotEmpty(trailerName)) {
						if (endHeaderframe.getMetaData().getFields().containsKey(trailerName)) {
							request.getFields().add(trailerName,
									endHeaderframe.getMetaData().getFields().get(trailerName));
							serverHTTPHandler.messageComplete(request, response, output, connection);
						} else {
							throw new IllegalArgumentException(
									"the stream " + stream.getId() + " received illegal meta data");
						}
					} else {
						throw new IllegalArgumentException(
								"the stream " + stream.getId() + " received illegal meta data");
					}
				} else {
					throw new IllegalArgumentException("the stream " + stream.getId() + " received illegal meta data");
				}
			}

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
			public void onReset(Stream stream, ResetFrame resetFrame) {
				ErrorCode errorCode = ErrorCode.from(resetFrame.getError());
				String reason = errorCode == null ? "error=" + resetFrame.getError() : errorCode.name().toLowerCase();
				serverHTTPHandler.badMessage(resetFrame.getError(), reason, request, response, output, connection);
			}

		};
	}

}
