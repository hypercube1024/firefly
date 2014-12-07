package com.firefly.net.tcp.aio;

import static com.firefly.net.tcp.TcpPerformanceParameter.BACKLOG;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.firefly.net.Config;
import com.firefly.net.Decoder;
import com.firefly.net.Encoder;
import com.firefly.net.EventManager;
import com.firefly.net.Handler;
import com.firefly.net.Server;
import com.firefly.net.event.DefaultEventManager;
import com.firefly.net.exception.NetException;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class AsynchronousTcpServer implements Server {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private Config config;
	private AtomicInteger id = new AtomicInteger();
	private AsynchronousServerSocketChannel serverSocketChannel;
	private AsynchronousTcpWorker worker;

	public AsynchronousTcpServer() {

	}
	public AsynchronousTcpServer(Decoder decoder, Encoder encoder, Handler handler) {
		config = new Config();
		config.setDecoder(decoder);
		config.setEncoder(encoder);
		config.setHandler(handler);
	}
	
	public AsynchronousTcpServer(Decoder decoder, Encoder encoder, Handler handler, int timeout) {
		config = new Config();
		config.setDecoder(decoder);
		config.setEncoder(encoder);
		config.setHandler(handler);
		config.setTimeout(timeout);
	}

	@Override
	public void setConfig(Config config) {
		this.config = config;
	}

	@Override
	public void start(String host, int port) {
		if (config == null)
			throw new NetException("server config is null");
		log.debug(config.toString());
		EventManager eventManager = new DefaultEventManager(config);
		worker = new AsynchronousTcpWorker(config, eventManager);
		bind(host, port);
		listen();
	}
	
	private void bind(String host, int port) {
		try {
			log.info("asychronous I/O thread pool [{}], [{}], [{}]", 
					config.getAsynchronousCorePoolSize(),
					config.getAsynchronousMaximumPoolSize(),
					config.getAsynchronousPoolKeepAliveTime());
			
			serverSocketChannel = AsynchronousServerSocketChannel.open(
					AsynchronousChannelGroup.withThreadPool(new ThreadPoolExecutor(
							config.getAsynchronousCorePoolSize(),
							config.getAsynchronousMaximumPoolSize(), 
							config.getAsynchronousPoolKeepAliveTime(), 
							TimeUnit.MILLISECONDS, 
							new LinkedTransferQueue<Runnable>(),
							new ThreadFactory(){

								@Override
								public Thread newThread(Runnable r) {
									return new Thread(r, "firefly asynchronous server thread");
								}
							})));
			serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			serverSocketChannel.bind(new InetSocketAddress(host, port), BACKLOG);
		} catch (Exception e) {
			log.error("ServerSocket bind error", e);
		}
	}
	
	private void listen() {
		serverSocketChannel.accept(id.getAndIncrement(), new CompletionHandler<AsynchronousSocketChannel, Integer>(){

			@Override
			public void completed(AsynchronousSocketChannel socketChannel, Integer sessionId) {
				try {
					worker.registerAsynchronousChannel(socketChannel, sessionId);
				} finally {
					listen();
				}
			}

			@Override
			public void failed(Throwable exc, Integer id) {
				try {
					log.error("server accepts channel " + id + " error occurs", exc);
				} finally {
					listen();
				}
			}} );
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

}
