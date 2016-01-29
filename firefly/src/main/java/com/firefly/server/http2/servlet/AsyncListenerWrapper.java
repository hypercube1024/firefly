package com.firefly.server.http2.servlet;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class AsyncListenerWrapper {

	private AsyncListener listener = null;
	private AsyncEvent asyncEvent;

	public AsyncListenerWrapper(AsyncContext asyncContext, AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
		this.listener = listener;
		this.asyncEvent = new AsyncEvent(asyncContext, servletRequest, servletResponse);
	}

	public void fireOnStartAsync() throws IOException {
		listener.onStartAsync(asyncEvent);
	}

	public void fireOnComplete() throws IOException {
		listener.onComplete(asyncEvent);
	}

	public void fireOnTimeout() throws IOException {
		listener.onTimeout(asyncEvent);
	}

	public void fireOnError() throws IOException {
		listener.onError(asyncEvent);
	}
}
