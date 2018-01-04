package com.firefly.codec.http2.stream;

import com.firefly.codec.common.ConnectionExtInfo;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.Connection;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;

public interface HTTPConnection extends Connection, ConnectionExtInfo {

    HttpVersion getHttpVersion();

    HTTPConnection onClose(Action1<HTTPConnection> closedCallback);

    HTTPConnection onException(Action2<HTTPConnection, Throwable> exceptionCallback);

}