package org.slf4j.impl;

import com.fireflysource.log.Log;
import com.fireflysource.log.LogFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogFactoryImpl implements ILoggerFactory, Closeable {

    private Map<String, Logger> map = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(String name) {
        Logger logger = map.get(name);
        if (logger != null) {
            return logger;
        } else {
            Log log = LogFactory.getInstance().getLog(name);
            if (log != null) {
                Logger newInstance = new LoggerImpl(log);
                Logger oldInstance = map.putIfAbsent(name, newInstance);
                return oldInstance == null ? newInstance : oldInstance;
            } else {
                return null;
            }
        }
    }

    @Override
    public void close() {
        LogFactory.getInstance().close();
    }
}
