package com.firefly.net;

import com.firefly.net.buffer.SocketSendBufferPool.SendBuffer;

public interface SendBufferPool {
	SendBuffer acquire(Object src);
}
