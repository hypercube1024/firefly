package com.firefly.net.tcp.aio;

import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;

public class AsynchronousTcpWorker implements Worker {
	private static Logger log = LoggerFactory.getLogger("firefly-system");

	private final Config config;
	private final EventManager eventManager;

	AsynchronousTcpWorker(Config config, EventManager eventManager) {
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
