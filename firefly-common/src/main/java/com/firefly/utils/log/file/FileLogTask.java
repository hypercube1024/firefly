package com.firefly.utils.log.file;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.log.LogItem;
import com.firefly.utils.log.LogTask;

public class FileLogTask implements LogTask {
	private volatile boolean start;
	private Queue<LogItem> queue = new ConcurrentLinkedQueue<LogItem>();
	private Thread thread = new Thread(this, "firefly log thread");
	
	public FileLogTask() {
		thread.setPriority(Thread.MIN_PRIORITY);
	}

	@Override
	public void run() {
		while (true) {
			write();
			if (!start && queue.isEmpty())
				break;

			try {
				Thread.sleep(500L);
			} catch (InterruptedException e) {
				e.printStackTrace();
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

	private void write() {
		for (LogItem logItem = null; (logItem = queue.poll()) != null;) {
			Log log = LogFactory.getInstance().getLog(logItem.getName());
			if (log instanceof FileLog) {
				((FileLog)log).write(logItem);
			}
		}
		LogFactory.getInstance().flush();
	}
}
