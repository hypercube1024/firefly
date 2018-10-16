package com.firefly.utils.log;

import com.firefly.utils.collection.TreeTrie;
import com.firefly.utils.collection.Trie;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.log.file.FileLog;
import com.firefly.utils.log.file.FileLogTask;
import com.firefly.utils.time.Millisecond100Clock;

public class LogFactory extends AbstractLifeCycle {

    private final Trie<Log> logTree = new TreeTrie<>();
    private final LogTask logTask;

    private static class Holder {
        private static LogFactory instance = new LogFactory();
    }

    public static LogFactory getInstance() {
        return Holder.instance;
    }

    private LogFactory() {
        logTask = new FileLogTask(logTree);

        LogConfigParser parser = new XmlLogConfigParser();
        boolean success = parser.parse((fileLog) -> logTree.put(fileLog.getName(), fileLog));

        if (!success) {
            System.out.println("log configuration parsing failure!");
        }

        if (logTree.get(LogConfigParser.DEFAULT_LOG_NAME) == null) {
            FileLog fileLog = parser.createDefaultLog();
            logTree.put(fileLog.getName(), fileLog);
        }

        start();
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

    public LogTask getLogTask() {
        return logTask;
    }

    @Override
    protected void init() {
        logTask.start();
    }

    @Override
    protected void destroy() {
        logTask.stop();
        Millisecond100Clock.stop();
    }

}
