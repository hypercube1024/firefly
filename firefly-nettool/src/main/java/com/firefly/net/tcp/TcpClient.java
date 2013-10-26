package com.firefly.net.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.firefly.net.Client;
import com.firefly.net.Config;
import com.firefly.net.Decoder;
import com.firefly.net.Encoder;
import com.firefly.net.EventManager;
import com.firefly.net.Handler;
import com.firefly.net.Worker;
import com.firefly.net.event.CurrentThreadEventManager;
import com.firefly.net.event.ThreadPoolEventManager;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public class TcpClient implements Client {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
    private Config config;
    private Worker[] workers;
    private Thread consumerThread;
    private Selector selector;
    private AtomicInteger sessionId = new AtomicInteger(0);
    private volatile boolean start = false;

    public TcpClient() {
    }

    public TcpClient(Decoder decoder, Encoder encoder, Handler handler) {
        this();
        config = new Config();
        config.setDecoder(decoder);
        config.setEncoder(encoder);
        config.setHandler(handler);
    }

    private synchronized Client init() throws IOException {
        if (start)
            return this;

        if (config == null)
            throw new IllegalArgumentException("init error config is null");
        
        EventManager eventManager = null;
        if (config.getHandleThreads() >= 0) {
			eventManager = new ThreadPoolEventManager(config);
		} else {
			eventManager = new CurrentThreadEventManager(config);
		}
        
        log.info("client worker num: {}", config.getWorkerThreads());
        workers = new Worker[config.getWorkerThreads()];
        for (int i = 0; i < config.getWorkerThreads(); i++)
            workers[i] = new TcpWorker(config, i, eventManager);
        
        selector = Selector.open();
        consumerThread = new Thread(new Consumer(), config.getClientName());
        start = true;
        consumerThread.start();
        return this;
    }

    @Override
    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public int connect(String host, int port) {
        int id = sessionId.getAndIncrement();
        connect(host, port, id);
        return id;
    }
    
    @Override
    public void connect(String host, int port, int id) {
    	try {
	    	if (!start)
	            init();
    	
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);      
            boolean finished = socketChannel.connect(new InetSocketAddress(host, port));
            if(finished) {
            	log.debug("connection {} is immediately complete", id);
            	if(socketChannel.isConnectionPending()) {
            		if(socketChannel.finishConnect()) {
            			log.info("connection {} has finished immediately", id);
            			accept(socketChannel, id);
            			return;
            		}
            	}
            	socketChannel.register(selector, SelectionKey.OP_CONNECT, id);
            } else {
            	socketChannel.register(selector, SelectionKey.OP_CONNECT, id);
            }
            
        } catch (IOException e) {
            log.error("connect error", e);
        }
    }
    
    private final class Consumer implements Runnable {

		@Override
		public void run() {
			while(start) {
				try {
					selector.select(1000);
					Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
					while (iterator.hasNext()) {   
						SelectionKey key = iterator.next();   
						iterator.remove();
						if (key.isConnectable()) {
							SocketChannel socketChannel = (SocketChannel)key.channel();   
                            if (socketChannel.isConnectionPending()) {
                            	if(socketChannel.finishConnect()) {
                            		int id = (Integer) key.attachment();
                            		log.info("connection {} has finished in select loop", id);
                            		accept(socketChannel, id);
                            	}
                            }
						}
					}
					
				} catch (IOException e) {
					log.error("Failed to create a connection.", e);
				}   
                
			}
		}
    }
    
    private void accept(SocketChannel socketChannel, int sessionId) {
        try {
            int workerIndex = Math.abs(sessionId) % workers.length;
            log.debug("accept sessionId [{}] and worker index [{}]", sessionId, workerIndex);
            workers[workerIndex].registerSelectableChannel(socketChannel, sessionId);
        } catch (Exception e) {
            log.error("Failed to initialize an accepted socket.", e);
            try {
                socketChannel.close();
            } catch (IOException e1) {
                log.error("Failed to close a partially accepted socket.", e1);
            }
        }
    }
    
	@Override
	public void shutdown() {
		for(Worker worker : workers)
			worker.shutdown();

		start = false;
		Millisecond100Clock.stop();
	}

}
