package com.firefly.server.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.firefly.utils.collection.LinkedTransferQueue;
import com.firefly.utils.collection.TransferQueue;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.HashTimeWheel;

public class AsyncContextImpl implements AsyncContext {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static final HashTimeWheel timeWheel = new HashTimeWheel();
	
	static {
		timeWheel.start();
	}
	
	private long timeout = -1;
	private boolean originalRequestAndResponse = true;
	private volatile boolean startAsync = false;
	private volatile boolean complete = false;
	private ServletRequest request;
	private ServletResponse response;
	private final List<AsyncListenerWrapper> listeners = new ArrayList<AsyncListenerWrapper>();
	private final TransferQueue<Future<?>> threadFutureList = new LinkedTransferQueue<Future<?>>();
	private HashTimeWheel.Future timeoutFuture;

	public boolean isStartAsync() {
		return startAsync;
	}
	
	public void startAsync(ServletRequest request, ServletResponse response, boolean originalRequestAndResponse, long t) {
		this.request = request;
		this.response = response;
		this.originalRequestAndResponse = originalRequestAndResponse;
		if(timeout == -1)
			timeout = t;
		
		fireOnStartAsync();
		startAsync = true;
		complete = false;
		
		if(timeout <= 0)
			return;
		
		if(timeoutFuture != null)
			return;
		
		timeoutFuture = timeWheel.add(timeout, new Runnable(){

			@Override
			public void run() {
				if(complete)
					return;
				
				Future<?> f = null;
				while( (f = threadFutureList.poll()) != null) {
					if(!f.isDone() && !f.isCancelled())
						f.cancel(true);
				}
				
				fireOnTimeout();
			}
		});
	}
	
	@Override
	public void complete() {
		timeoutFuture.cancel();
		fireOnComplete();
		startAsync = false;
		complete = true;
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
		HttpServletRequest sr = (HttpServletRequest)getRequest();
        String path = sr.getRequestURI();
		dispatch(path);
	}

	@Override
	public void dispatch(String path) {
		dispatch(null, path);
	}

	@Override
	public void dispatch(ServletContext context, String path) {
		complete();
		try {
			request.getRequestDispatcher(path).forward(request, response);
		} catch (Throwable e) {
			log.error("async dispatch exception", e);
			fireOnError();
		}
	}

	@Override
	public void start(Runnable run) {
		Future<?> future = ThreadPoolWrapper.getExecutorService().submit(run);
		threadFutureList.offer(future);
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
	
	private void fireOnStartAsync() {
		List<AsyncListenerWrapper> listenersCopy = getListenersCopy();
		for (AsyncListenerWrapper listener : listenersCopy) {
			try {
				listener.fireOnStartAsync();
			} catch (IOException e) {
				log.error("async start event error", e);
				fireOnError();
			}
		}
	}
	
	private void fireOnComplete() {
		List<AsyncListenerWrapper> listenersCopy = getListenersCopy();
		for (AsyncListenerWrapper listener : listenersCopy) {
			try {
				listener.fireOnComplete();
			} catch (IOException e) {
				log.error("async complete event error", e);
				fireOnError();
			}
		}
	}
	
	private void fireOnTimeout() {
		List<AsyncListenerWrapper> listenersCopy = getListenersCopy();
		for (AsyncListenerWrapper listener : listenersCopy) {
			try {
				listener.fireOnTimeout();
			} catch (IOException e) {
				log.error("async timeout event error", e);
				fireOnError();
			}
		}
	}
	
	private void fireOnError() {
		List<AsyncListenerWrapper> listenersCopy = getListenersCopy();
		for (AsyncListenerWrapper listener : listenersCopy) {
			try {
				listener.fireOnError();
			} catch (IOException e) {
				log.error("async error event exception", e);
			}
		}
	}
	
	private List<AsyncListenerWrapper> getListenersCopy() {
		List<AsyncListenerWrapper> listenersCopy = new ArrayList<AsyncListenerWrapper>(listeners);
		return listenersCopy;
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

}
