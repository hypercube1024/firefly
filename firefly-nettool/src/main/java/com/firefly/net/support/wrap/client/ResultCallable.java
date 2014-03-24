package com.firefly.net.support.wrap.client;

import java.util.concurrent.Callable;

public class ResultCallable<V> implements Callable<V> {
	
	private V value;
	
	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public V call() throws Exception {
		return value;
	}

}
