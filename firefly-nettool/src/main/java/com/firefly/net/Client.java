package com.firefly.net;

public interface Client {
	
	void setConfig(Config config);
	
	int connect(String host, int port);
	
	void connect(String host, int port, int id);
	
	void shutdown();
}
