package com.firefly.utils.log.file;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.collection.Trie;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.log.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

public class FileLogTask extends AbstractLifeCycle implements LogTask {

    public static final long flushInterval = Long.getLong("com.firefly.utils.log.FileLogTask.interval", 1000L);

    private BlockingQueue<LogItem> queue = new LinkedTransferQueue<>();
    private Thread thread = new Thread(this, "firefly asynchronous log thread");
    private final Trie<Log> logTree;

    public FileLogTask(Trie<Log> logTree) {
        thread.setPriority(Thread.MIN_PRIORITY);
        this.logTree = logTree;
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

    private void intervalFlushAll() {
        for (String key : logTree.keySet()) {
            FileLog fileLog = getFileLog(key);
            if (fileLog != null) {
                fileLog.intervalFlush();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                for (LogItem logItem; (logItem = queue.poll(flushInterval, TimeUnit.MILLISECONDS)) != null; ) {
                    try {
                        FileLog fileLog = getFileLog(logItem.getName());
                        if (fileLog != null) {
                            fileLog.write(logItem);
                        }
                    } catch (Throwable e) {
                        System.err.println("write log exception, " + e.getMessage());
                    }
                }
                intervalFlushAll();
            } catch (Throwable e) {
                System.err.println("write log exception, " + e.getMessage());
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
