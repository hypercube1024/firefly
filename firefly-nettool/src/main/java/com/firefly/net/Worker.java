package com.firefly.net;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public interface Worker extends Runnable {

	void registerSelectableChannel(SelectableChannel selectableChannel, int sessionId);

	int getWorkerId();

	void shutdown();
}
