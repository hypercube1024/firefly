package com.fireflysource.net.utils;

import com.fireflysource.common.lifecycle.AbstractLifeCycle;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

abstract public class LifeCycleUtils {

    public static void stopAll() {
        AbstractLifeCycle.stopAll();
        ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
        if (iLoggerFactory instanceof Closeable) {
            System.out.println(iLoggerFactory.getClass());
            try {
                ((Closeable) iLoggerFactory).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
