package com.firefly.net.tcp.codec.ffsocks.stream;

import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;

/**
 * @author Pengtao Qiu
 */
public interface FfsocksServerConnection extends FfsocksConnection {

    FfsocksServerConnection metaInfoComplete(Action1<FfsocksServerContext> action);

    FfsocksServerConnection content(Action2<FfsocksServerContext, byte[]> action);

    FfsocksServerConnection messageComplete(Action1<FfsocksServerContext> action);

}
