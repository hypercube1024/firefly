package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.utils.concurrent.Promise;

public interface ClientHTTPConnection extends HTTPConnection {
	
	public void upgradeHTTP2WithCleartext(MetaData.Request request, SettingsFrame settings,
			final Promise<HTTPConnection> promise, final Promise<Stream> initStream,
			final Stream.Listener initStreamListener, final Listener listener, final ClientHTTPHandler handler);

	public HTTPOutputStream requestWith100Continue(MetaData.Request request, ClientHTTPHandler handler);

	public void request(MetaData.Request request, ClientHTTPHandler handler);

	public void request(MetaData.Request request, ByteBuffer buffer, ClientHTTPHandler handler);

	public void request(MetaData.Request request, ByteBuffer[] buffers, ClientHTTPHandler handler);

	public HTTPOutputStream requestWithStream(MetaData.Request request, ClientHTTPHandler handler);
}
