package com.firefly.net.tcp;

import java.util.List;

import com.firefly.net.tcp.aio.AsynchronousTcpClient;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Func0;
import com.firefly.utils.lang.AbstractLifeCycle;

public class SimpleTcpClient extends AbstractLifeCycle {

	// ALPN callback
	protected Action0 alpnUnsupported;
	protected Action1<String> alpnSelected;
	protected Func0<List<String>> alpnProtocols;

	protected AsynchronousTcpClient client;
	protected TcpConfiguration config;

	public SimpleTcpClient() {
		this(new TcpConfiguration());
	}

	public SimpleTcpClient(TcpConfiguration config) {
		client = new AsynchronousTcpClient(config);
		this.config = config;
	}

	public class ClientContext {
		Promise<TcpConnection> promise;
		TcpConnection connection;
	}

	public SimpleTcpClient alpnUnsupported(Action0 alpnUnsupported) {
		this.alpnUnsupported = alpnUnsupported;
		return this;
	}

	public SimpleTcpClient alpnSelected(Action1<String> alpnSelected) {
		this.alpnSelected = alpnSelected;
		return this;
	}

	public SimpleTcpClient alpnProtocols(Func0<List<String>> alpnProtocols) {
		this.alpnProtocols = alpnProtocols;
		return this;
	}

	public FuturePromise<TcpConnection> connect(String host, int port) {
		FuturePromise<TcpConnection> promise = new FuturePromise<>();
		connect(host, port, promise);
		return promise;
	}

	public void connect(String host, int port, Action1<TcpConnection> conn) {
		Promise<TcpConnection> promise = new Promise<TcpConnection>() {

			@Override
			public void succeeded(TcpConnection result) {
				conn.call(result);
			}
		};
		connect(host, port, promise);
	}

	public void connect(String host, int port, Action1<TcpConnection> conn, Action1<Throwable> failed) {
		Promise<TcpConnection> promise = new Promise<TcpConnection>() {

			@Override
			public void succeeded(TcpConnection result) {
				conn.call(result);
			}

			public void failed(Throwable x) {
				failed.call(x);
			}
		};
		connect(host, port, promise);
	}

	public void connect(String host, int port, Promise<TcpConnection> promise) {

	}

	@Override
	protected void init() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void destroy() {
		client.stop();
	}

}
