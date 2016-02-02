package com.firefly.server.http2.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ServletServerHTTPHandler extends ServerHTTPHandler.Adapter {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final HTTP2Configuration http2Configuration;
	private final HttpServletDispatcherController controller;

	public ServletServerHTTPHandler(HTTP2Configuration http2Configuration, HttpServletDispatcherController controller) {
		this.http2Configuration = http2Configuration;
		this.controller = controller;
		AsyncContextImpl.init(http2Configuration);
	}

	@Override
	public boolean headerComplete(Request request, Response response, HTTPOutputStream output,
			HTTPConnection connection) {
		HTTPServletRequestImpl servletRequest = new HTTPServletRequestImpl(http2Configuration, request, response,
				output, connection);
		request.setAttachment(servletRequest);
		return false;
	}

	@Override
	public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
			HTTPConnection connection) {
		HTTPServletRequestImpl servletRequest = (HTTPServletRequestImpl) request.getAttachment();
		try {
			servletRequest.getBodyPipedStream().getOutputStream().write(BufferUtils.toArray(item));
		} catch (IOException e) {
			log.error("receive http body data exception", e);
		}
		return false;
	}

	@Override
	public boolean messageComplete(Request request, Response response, HTTPOutputStream output,
			HTTPConnection connection) {
		try (HTTPServletRequestImpl servletRequest = (HTTPServletRequestImpl) request.getAttachment()) {
			servletRequest.completeDataReceiving();
			controller.dispatch(servletRequest, servletRequest.getResponse());
		}
		return true;
	}

	@Override
	public void badMessage(int status, String reason, Request request, Response response, HTTPOutputStream output,
			HTTPConnection connection) {
		HTTPServletRequestImpl servletRequest = (HTTPServletRequestImpl) request.getAttachment();
		try {
			servletRequest.getResponse().sendError(status, reason);
		} catch (IOException e) {
			log.error("response bad message exception", e);
		}
	}

}
