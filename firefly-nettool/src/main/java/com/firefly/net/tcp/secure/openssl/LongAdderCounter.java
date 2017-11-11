package com.firefly.net.tcp.secure.openssl;

import java.util.concurrent.atomic.LongAdder;

final class LongAdderCounter extends LongAdder implements LongCounter {

    @Override
    public long value() {
        return longValue();
    }
}
