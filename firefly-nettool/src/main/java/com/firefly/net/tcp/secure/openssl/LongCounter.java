package com.firefly.net.tcp.secure.openssl;

/**
 * Counter for long.
 */
public interface LongCounter {
    void add(long delta);

    void increment();

    void decrement();

    long value();
}
