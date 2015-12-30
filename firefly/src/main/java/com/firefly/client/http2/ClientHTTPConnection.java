package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;

public interface ClientHTTPConnection extends HTTPConnection {

	public void request(MetaData.Request request, ClientHTTPHandler handler);

	public void request(MetaData.Request request, ByteBuffer buffer, ClientHTTPHandler handler);

	public void request(MetaData.Request request, ByteBuffer[] buffers, ClientHTTPHandler handler);

	public HTTPOutputStream requestWithStream(MetaData.Request request, ClientHTTPHandler handler);
}
