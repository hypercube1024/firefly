package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.concurrent.Promise;

public interface HTTPClientConnection extends HTTPConnection {

	public void request(MetaData.Request request, ClientHTTPHandler handler);

	public void request(MetaData.Request request, ByteBuffer buffer, ClientHTTPHandler handler);

	public void request(MetaData.Request request, ByteBuffer[] buffers, ClientHTTPHandler handler);
	
	public HTTPOutputStream requestWithContinuation(MetaData.Request request, ClientHTTPHandler handler);

	public HTTPOutputStream requestWithStream(MetaData.Request request, ClientHTTPHandler handler);

	public void requestWithStream(MetaData.Request request, Promise<HTTPOutputStream> promise,
			ClientHTTPHandler handler);

	public void upgradeHTTP2WithCleartext(final MetaData.Request request, final SettingsFrame settings,
			final Promise<HTTPClientConnection> promise, final ClientHTTPHandler handler);
}
