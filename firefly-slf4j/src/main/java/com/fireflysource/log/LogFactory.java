package com.fireflysource.log;

import com.fireflysource.log.internal.utils.collection.TreeTrie;
import com.fireflysource.log.internal.utils.collection.Trie;

import java.io.Closeable;
import java.io.IOException;

public class LogFactory implements Closeable {

    private final Trie<Log> logTree = new TreeTrie<>();

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
        logTree.keySet().forEach(k -> {
            Log log = logTree.get(k);
            try {
                log.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static class Holder {
        private static LogFactory instance = new LogFactory();
    }

}
