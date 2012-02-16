package com.firefly.net;

public interface Server {
	void setConfig(Config config);
	
	void start(String host, int port);
	
	void shutdown();
}
