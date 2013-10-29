package com.firefly.net.support.wrap.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.firefly.net.Client;
import com.firefly.net.Decoder;
import com.firefly.net.Encoder;
import com.firefly.net.support.SynchronousObject;
import com.firefly.net.tcp.TcpClient;

public class SimpleTcpClient {
	private String host;
	private int port;
	private SimpleTcpClientHandler handler;

	private Map<Integer, ConnectionInfo> connectionInfo = new ConcurrentHashMap<Integer,ConnectionInfo>();
	private Client client;
	private AtomicInteger sessionId = new AtomicInteger(0);

	public SimpleTcpClient(String host, int port, Decoder decoder, Encoder encoder, SimpleTcpClientHandler handler) {
		this.host = host;
		this.port = port;
		this.handler = handler;
		client = new TcpClient(decoder, encoder, handler);
	}

	public TcpConnection connect() {
		return connect(0);
	}
	
	public TcpConnection connect(long timeout) {
		final long t = timeout <= 0 ? 5000L : timeout;
		final SynchronousObject<TcpConnection> ret = new SynchronousObject<TcpConnection>();
		connect(timeout, new SessionOpenedCallback(){

			@Override
			public void sessionOpened(TcpConnection connection) {
				ret.put(connection, t);
			}
		});
		return ret.get(t);
	}
	
	public void connect(long timeout, SessionOpenedCallback callback) {
		handler.setConnectionInfo(connectionInfo);
		
		int id = sessionId.getAndIncrement();
		ConnectionInfo info = new ConnectionInfo();
		info.callback = callback;
		info.timeout = timeout;
		connectionInfo.put(id, info);
		client.connect(host, port, id);
	}

	public void shutdown() {
		client.shutdown();
	}
}
