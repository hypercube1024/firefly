package com.firefly.net.support;

import com.firefly.net.Client;
import com.firefly.net.Decoder;
import com.firefly.net.Encoder;
import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.net.tcp.TcpClient;

public class SimpleTcpClient {
	private String host;
	private int port;

	private Synchronizer<Session> synchronizer = new Synchronizer<Session>();
	private Client client;

	public SimpleTcpClient(String host, int port, Decoder decoder,
			Encoder encoder, Handler handler) {
		this.host = host;
		this.port = port;
		client = new TcpClient(decoder, encoder,
				handler == null ? new SimpleTcpClientHandler(synchronizer)
						: handler);
	}

	public SimpleTcpClient(String host, int port, Decoder decoder,
			Encoder encoder) {
		this(host, port, decoder, encoder, null);
	}

	public TcpConnection connect() {
		return connect(0);
	}
	
	public TcpConnection connect(long timeout) {
		int id = client.connect(host, port);
		TcpConnection ret = new TcpConnection(synchronizer.get(id), timeout);
		return ret;
	}

	public void shutdown() {
		client.shutdown();
	}
}
