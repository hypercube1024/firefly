package com.firefly.net.tcp;

public interface TcpPerformanceParameter {
	int CONNECTION_TIME = 1;
	int LATENCY = 2;
	int BANDWIDTH = 0;
	int BACKLOG = 1024 * 16;

	int CLEANUP_INTERVAL = 256;
	int WRITE_SPIN_COUNT = 16;
//	int WRITE_BUFFER_HIGH_WATER_MARK = 64 * 1024;
//	int WRITE_BUFFER_LOW_WATER_MARK = 32 * 1024;
}
