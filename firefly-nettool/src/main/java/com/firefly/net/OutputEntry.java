package com.firefly.net;

import com.firefly.utils.concurrent.Callback;

public interface OutputEntry<T> {
	
	public OutputEntryType getOutputEntryType();
	
	public Callback getCallback();
	
	public T getData();
}
