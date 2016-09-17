package com.firefly.net.tcp.codec;

import com.firefly.utils.function.Action1;

public interface MessageHandler<R, T> {
	
	public void receive(R obj);

	public void complete(Action1<T> complete);

}
