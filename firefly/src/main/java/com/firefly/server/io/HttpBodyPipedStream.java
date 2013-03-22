package com.firefly.server.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface HttpBodyPipedStream extends Closeable {

	InputStream getInputStream() throws IOException;
	
	OutputStream getOutputStream() throws IOException;
}
