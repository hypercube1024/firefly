package com.firefly.server.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class AsyncContextImpl implements AsyncContext {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private long timeout = -1;
	private boolean originalRequestAndResponse = true;
	private boolean startAsync = false;
	private ServletRequest request;
	private ServletResponse response;
	private List<AsyncListenerWrapper> listeners = new ArrayList<AsyncListenerWrapper>();

	public void startAsync(ServletRequest request, ServletResponse response, boolean originalRequestAndResponse, long timeout) {
		this.request = request;
		this.response = response;
		this.originalRequestAndResponse = originalRequestAndResponse;
		this.timeout = timeout;
		
		for (AsyncListenerWrapper listener : listeners) {
			try {
				listener.fireOnStartAsync();
			} catch (IOException e) {
				log.error("async start event error", e);
			}
		}
		
		startAsync = true;
	}

	@Override
	public ServletRequest getRequest() {
		return request;
	}

	@Override
	public ServletResponse getResponse() {
		return response;
	}

	@Override
	public boolean hasOriginalRequestAndResponse() {
		return originalRequestAndResponse;
	}

	@Override
	public void dispatch() {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispatch(String path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispatch(ServletContext context, String path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void complete() {
		// TODO Auto-generated method stub

	}

	@Override
	public void start(Runnable run) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addListener(AsyncListener listener) {
        addListener(listener, request, response);
	}

	@Override
	public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
		AsyncListenerWrapper wrapper = new AsyncListenerWrapper(this, listener, servletRequest, servletResponse);
        listeners.add(wrapper);
	}

	@Override
	public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
		T listener = null;
		try {
			listener = clazz.newInstance();
		} catch (Throwable e) {
			log.error("create async listener error", e);
		}
		return listener;
	}

	@Override
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public long getTimeout() {
		return timeout;
	}

	public boolean isStartAsync() {
		return startAsync;
	}

}
