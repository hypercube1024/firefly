package com.firefly.utils.log.file;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.log.LogItem;
import com.firefly.utils.log.LogTask;

public class FileLogTask implements LogTask {
	private volatile boolean start;
	private BlockingQueue<LogItem> queue = new LinkedTransferQueue<>();
	private Thread thread = new Thread(this, "firefly log thread");
	private final Map<String, Log> logMap;

	public FileLogTask(Map<String, Log> logMap) {
		thread.setPriority(Thread.MIN_PRIORITY);
		this.logMap = logMap;
	}

	@Override
	public void run() {
		while (true) {
			try {
				for (LogItem logItem = null; (logItem = queue.poll(1000, TimeUnit.MILLISECONDS)) != null;) {
					Log log = LogFactory.getInstance().getLog(logItem.getName());
					if (log instanceof FileLog) {
						((FileLog) log).write(logItem);
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
			LogFactory.getInstance().flush();

			if (!start && queue.isEmpty()) {
				for (Entry<String, Log> entry : logMap.entrySet()) {
					Log log = entry.getValue();
					if (log instanceof FileLog) {
						try {
							((FileLog) log).close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				break;
			}
		}
	}

	@Override
	public void start() {
		if (!start) {
			synchronized (this) {
				if (!start) {
					start = true;
					thread.start();
				}
			}
		}
	}

	@Override
	public void shutdown() {
		start = false;
	}

	@Override
	public void add(LogItem logItem) {
		if (!start)
			return;

		if (VerifyUtils.isEmpty(logItem.getName()))
			throw new IllegalArgumentException("log name is empty");

		queue.offer(logItem);
	}
}
