package com.firefly.codec.http2.stream;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.model.MetaData;

public interface HTTPHandler {

	public boolean content(ByteBuffer item, MetaData.Request request, MetaData.Response response,
			HTTPOutputStream output, HTTPConnection connection);

	public boolean headerComplete(MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
			HTTPConnection connection);

	public boolean messageComplete(MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
			HTTPConnection connection);

	public void badMessage(int status, String reason, MetaData.Request request, MetaData.Response response,
			HTTPOutputStream output, HTTPConnection connection);

	public void earlyEOF(MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
			HTTPConnection connection);

}
