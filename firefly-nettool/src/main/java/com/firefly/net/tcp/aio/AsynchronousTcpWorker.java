package com.firefly.net.tcp.aio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;

import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.Worker;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class AsynchronousTcpWorker implements Worker {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final Config config;
	final EventManager eventManager;

	public AsynchronousTcpWorker(Config config, EventManager eventManager) {
		this.config = config;
		this.eventManager = eventManager;
	}

	@Override
	public void registerChannel(Channel channel, int sessionId) {
		try {
			AsynchronousSocketChannel socketChannel = (AsynchronousSocketChannel) channel;
			socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);

			AsynchronousTcpSession session = new AsynchronousTcpSession(sessionId, config, eventManager, socketChannel);
			eventManager.executeOpenTask(session);
			session._read();
		} catch (IOException e) {
			log.error("socketChannel register error", e);
		}
	}

	@Override
	public void shutdown() {
	}

}
