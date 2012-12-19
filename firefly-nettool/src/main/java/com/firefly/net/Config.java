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
	 * @return 连接超时时间，单位ms
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * 设置连接超时时间
	 * 
	 * @param timeout
	 *            超时时间，单位ms
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * @return handler执行线程数
	 */
	public int getHandleThreads() {
		return handleThreads;
	}

	/**
	 * 设置handler执行线程数 <br/>
	 * 线程数>0: 使用固定线程数线程池。<br/>
	 * 线程数=0: 使用cached线程池 。<br/>
	 * 线程数<0: handler在worker线程执行。<br/>
	 * 
	 * @param handleThreads
	 *            handler执行线程数
	 */
	public void setHandleThreads(int handleThreads) {
		this.handleThreads = handleThreads;
	}

	/**
	 * @return 接受数据ByteBuffer大小
	 */
	public int getReceiveByteBufferSize() {
		return receiveByteBufferSize;
	}

	/**
	 * 设置接受数据ByteBuffer大小，当设置为小于或等于0时，使用自适应buffer大小
	 * @param receiveByteBufferSize 设置接受数据ByteBuffer大小
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
