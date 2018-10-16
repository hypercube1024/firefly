package com.firefly.utils.log;

import com.firefly.utils.lang.LifeCycle;

public interface LogTask extends Runnable, LifeCycle {

    void add(LogItem logItem);

}
