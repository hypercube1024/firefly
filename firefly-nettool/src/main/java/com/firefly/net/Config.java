package com.firefly.net;

public class Config {
	
	private int timeout = 10 * 1000;
	private int handleThreads = -1;
	private int receiveByteBufferSize = 0;
	private int workerThreads;
	{
		int workers = Runtime.getRuntime().availableProcessors();
		if (workers > 4)
			workerThreads = workers * 2;
		else
			workerThreads = workers + 1;
	}

	private String serverName = "firefly-server";
	private String clientName = "firefly-client";

	private Decoder decoder;
	private Encoder encoder;
	private Handler handler;

	/**
	 * The max I/O idle time, the default value is 10 seconds.
	 * 
	 * @return The max I/O idle time，the unit is MS.
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * The I/O timeout, if the last I/O timestamp before  present over timeout value, the session will close. 
	 * 
	 * @param timeout the max I/O idle time，the unit is MS.
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public int getHandleThreads() {
		return handleThreads;
	}

	/**
	 * Set thread number of handler thread pool <br/>
	 * if the number greater than 0, the handler will use the fixed number thread pool.<br/>
	 * if the number equals 0, the handler will use the cache thread pool.<br/>
	 * if the number less than 0, the handler will execute in worker thread<br/>
	 * 
	 * @param handleThreads thread number of handler thread pool
	 */
	public void setHandleThreads(int handleThreads) {
		this.handleThreads = handleThreads;
	}

	public int getReceiveByteBufferSize() {
		return receiveByteBufferSize;
	}

	/**
	 * Set the the buffer that is used to receive net data size, if the value less than 0 or equals 0
	 * it will use the adaptive buffer size
	 *  
	 * @param receiveByteBufferSize the buffer size
	 */
	public void setReceiveByteBufferSize(int receiveByteBufferSize) {
		this.receiveByteBufferSize = receiveByteBufferSize;
	}

	public int getWorkerThreads() {
		return workerThreads;
	}

	public void setWorkerThreads(int workerThreads) {
		this.workerThreads = workerThreads;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public Decoder getDecoder() {
		return decoder;
	}

	public void setDecoder(Decoder decoder) {
		this.decoder = decoder;
	}

	public Encoder getEncoder() {
		return encoder;
	}

	public void setEncoder(Encoder encoder) {
		this.encoder = encoder;
	}

	public Handler getHandler() {
		return handler;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

}
