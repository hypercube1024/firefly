package com.firefly.server.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ByteArrayHttpBodyPipedStream implements HttpBodyPipedStream {
	
	private ByteArrayOutputStream out;
	private ByteArrayInputStream in;
	private int length;
	
	public ByteArrayHttpBodyPipedStream(int length) {
		if(length >= Integer.MAX_VALUE)
			throw new IllegalArgumentException("http body length too larger");
		
		this.length = length;
	}

	@Override
	public void close() throws IOException {
		in = null;
		out = null;
	}

	@Override
	public InputStream getInputStream() {
		if(in == null) {
			in = new ByteArrayInputStream(out.toByteArray());
			out = null;
		}
		return in;
	}

	@Override
	public OutputStream getOutputStream() {
		if(out == null) {
			out = new ByteArrayOutputStream(length);
		}
		return out;
	}

}
