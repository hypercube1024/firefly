package com.firefly.client.http2;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPHandler;
import com.firefly.codec.http2.stream.HTTPOutputStream;

public interface ClientHTTPHandler extends HTTPHandler {

	public void continueToSendData(MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
			HTTPConnection connection);

	public static class Adapter implements ClientHTTPHandler {

		@Override
		public boolean content(ByteBuffer item, Request request, Response response, HTTPConnection connection) {
			return false;
		}

		@Override
		public boolean headerComplete(Request request, Response response, HTTPConnection connection) {
			return false;
		}

		@Override
		public boolean messageComplete(Request request, Response response, HTTPConnection connection) {
			return true;
		}

		@Override
		public void badMessage(int status, String reason, Request request, Response response,
				HTTPConnection connection) {
		}

		@Override
		public void earlyEOF(Request request, Response response, HTTPConnection connection) {
		}

		@Override
		public void continueToSendData(Request request, Response response, HTTPOutputStream output,
				HTTPConnection connection) {
		}

	}
}
