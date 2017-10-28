package com.firefly.net;

import com.firefly.utils.lang.LifeCycle;

import java.util.concurrent.ExecutorService;

public interface Server extends LifeCycle {

    void setConfig(Config config);

    void listen(String host, int port);

    ExecutorService getNetExecutorService();
}
