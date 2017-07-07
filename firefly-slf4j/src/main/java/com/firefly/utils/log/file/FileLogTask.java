package com.firefly.utils.log.file;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.collection.Trie;
import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.log.*;
import com.firefly.utils.time.Millisecond100Clock;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

public class FileLogTask extends AbstractLifeCycle implements LogTask {

    public static final long maxLogFlushInterval = Long.getLong("com.firefly.utils.log.file.maxLogFlushInterval", 1000L);

    private BlockingQueue<LogItem> queue = new LinkedTransferQueue<>();
    private Thread thread = new Thread(this, "firefly asynchronous log thread");
    private final Trie<Log> logTree;

    public FileLogTask(Trie<Log> logTree) {
        thread.setPriority(Thread.MIN_PRIORITY);
        this.logTree = logTree;
    }

    private long flushAllPerSecond(final long lastFlushedTime) {
        // flush all log buffer per one second
        long timeDifference = Millisecond100Clock.currentTimeMillis() - lastFlushedTime;
        if (timeDifference > maxLogFlushInterval) {
            flushAll();
            return Millisecond100Clock.currentTimeMillis();
        } else {
            return lastFlushedTime;
        }
    }

    public void flushAll() {
        for (String key : logTree.keySet()) {
            FileLog fileLog = getFileLog(key);
            if (fileLog != null) {
                fileLog.flush();
            }
        }
    }

    private FileLog getFileLog(String name) {
        Log log = LogFactory.getInstance().getLog(name);
        if (log instanceof ClassNameLogWrap) {
            ClassNameLogWrap classNameLogWrap = (ClassNameLogWrap) log;
            if (classNameLogWrap.getLog() instanceof FileLog) {
                return (FileLog) classNameLogWrap.getLog();
            }
        }
        return null;
    }

    @Override
    public void run() {
        long lastFlushedTime = Millisecond100Clock.currentTimeMillis();
        while (true) {
            try {
                for (LogItem logItem; (logItem = queue.poll(maxLogFlushInterval, TimeUnit.MILLISECONDS)) != null; ) {
                    FileLog fileLog = getFileLog(logItem.getName());
                    if (fileLog != null) {
                        fileLog.write(logItem);
                    }
                    lastFlushedTime = flushAllPerSecond(lastFlushedTime);
                }

                lastFlushedTime = flushAllPerSecond(lastFlushedTime);
            } catch (Throwable e) {
                System.err.println("write log exception, " + e.getMessage());
                // avoid CPU exhausting
                ThreadUtils.sleep(1000L);
            }

            if (!start && queue.isEmpty()) {
                for (String key : logTree.keySet()) {
                    FileLog fileLog = getFileLog(key);
                    if (fileLog != null) {
                        fileLog.close();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void add(LogItem logItem) {
        if (!start)
            return;

        if (VerifyUtils.isEmpty(logItem.getName()))
            throw new IllegalArgumentException("log name is empty");

        queue.offer(logItem);
    }

    @Override
    protected void init() {
        start = true;
        thread.start();
    }

    @Override
    protected void destroy() {
        start = false;
    }
}
