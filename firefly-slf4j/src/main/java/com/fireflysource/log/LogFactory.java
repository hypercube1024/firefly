package com.fireflysource.log;

import com.fireflysource.log.internal.utils.collection.TreeTrie;
import com.fireflysource.log.internal.utils.collection.Trie;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogFactory implements Closeable {

    private final Trie<Log> logTree = new TreeTrie<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private LogFactory() {
        LogConfigParser parser = new XmlLogConfigParser();
        boolean success = parser.parse((fileLog) -> logTree.put(fileLog.getName(), fileLog));

        if (!success) {
            System.out.println("log configuration parsing failure!");
        }

        if (logTree.get(LogConfigParser.DEFAULT_LOG_NAME) == null) {
            FileLog fileLog = parser.createDefaultLog();
            logTree.put(fileLog.getName(), fileLog);
        }
    }

    public static LogFactory getInstance() {
        return Holder.instance;
    }

    public Log getLog(Class<?> clazz) {
        return getLog(clazz.getName());
    }

    public Log getLog(String name) {
        Log log = logTree.getBest(name);
        if (log == null) {
            log = logTree.get(LogConfigParser.DEFAULT_LOG_NAME);
        }
        return new ClassNameLogWrap(log, name);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            logTree.keySet().forEach(k -> {
                Log log = logTree.get(k);
                try {
                    log.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


            ExecutorService pool = FileLog.Companion.getExecutor();
            long timeout = 15;
            TimeUnit unit = TimeUnit.SECONDS;
            pool.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!pool.awaitTermination(timeout, unit)) {
                    pool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!pool.awaitTermination(timeout, unit))
                        System.err.println("Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                pool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
            System.out.println("The file log thread is shutdown");
        }
    }

    private static class Holder {
        private static LogFactory instance = new LogFactory();
    }

}
