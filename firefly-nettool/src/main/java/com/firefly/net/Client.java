package com.firefly.net;

import com.firefly.utils.lang.LifeCycle;

import java.util.concurrent.ExecutorService;

public interface Client extends LifeCycle {

    void setConfig(Config config);

    int connect(String host, int port);

    void connect(String host, int port, int id);

    ExecutorService getNetExecutorService();

}
