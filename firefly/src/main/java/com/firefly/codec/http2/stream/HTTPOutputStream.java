package com.firefly.codec.http2.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class HTTPOutputStream extends OutputStream {
	
	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");
	
	protected final boolean clientMode;
	protected final MetaData info;
	protected boolean closed;
	protected boolean commited;
	
	public HTTPOutputStream(MetaData info, boolean clientMode) {
		this.info = info;
		this.clientMode = clientMode;
	}
	
	public synchronized boolean isClosed() {
		return closed;
	}

	public synchronized boolean isCommited() {
		return commited;
	}
	
	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b });
	}

	@Override
	public void write(byte[] array, int offset, int length) throws IOException {
		write(ByteBuffer.wrap(array, offset, length));
	}
	
	abstract public void writeWithContentLength(ByteBuffer[] data) throws IOException;
	
	abstract public void writeWithContentLength(ByteBuffer data) throws IOException;
	
	abstract public void commit() throws IOException;
	
	abstract public void write(ByteBuffer data) throws IOException;
	
	abstract public void write(ByteBuffer data, boolean endStream) throws IOException;
}
