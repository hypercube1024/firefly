package com.firefly.net;

import com.firefly.utils.concurrent.Callback;

public interface OutputEntry<T> {

    OutputEntryType getOutputEntryType();

    Callback getCallback();

    T getData();

    long remaining();
}
