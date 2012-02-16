package com.firefly.server.io;

import java.io.IOException;

import com.firefly.server.http.HttpServletRequestImpl;
import com.firefly.server.http.HttpServletResponseImpl;

public class ChunkedOutputStream extends HttpServerOutpuStream {

	private byte[] crlf, endFlag;
	private boolean chunked;

	public ChunkedOutputStream(int bufferSize,
			NetBufferedOutputStream bufferedOutput,
			HttpServletRequestImpl request, HttpServletResponseImpl response) {
		super(bufferSize, bufferedOutput, request, response);
		crlf = response.stringToByte("\r\n");
		endFlag = response.stringToByte("0\r\n\r\n");
	}
	
	@Override
	public void write(int b) throws IOException {
		super.write(b);
		if (size > bufferSize)
			flush();
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		super.write(b, off, len);
		if (size > bufferSize)
			flush();
	}

	@Override
	public void flush() throws IOException {
		if (!response.isCommitted()) {
			chunked = true;
			response.setHeader("Transfer-Encoding", "chunked");
			bufferedOutput.write(response.getHeadData());
			response.setCommitted(true);
		}

		if (size > 0) {
			bufferedOutput.write(response.getChunkedSize(size));
			for (ChunkedData d = null; (d = queue.poll()) != null;)
				d.write();
			bufferedOutput.write(crlf);
			size = 0;
		}
	}

	@Override
	public void close() throws IOException {
		if (!response.isCommitted()) {
			response.setHeader("Content-Length", String.valueOf(size));
			bufferedOutput.write(response.getHeadData());
			response.setCommitted(true);
		}

		if (size > 0) {
			if (chunked)
				bufferedOutput.write(response.getChunkedSize(size));
			for (ChunkedData d = null; (d = queue.poll()) != null;)
				d.write();
			if (chunked)
				bufferedOutput.write(crlf);
			size = 0;
		}

		if (chunked)
			bufferedOutput.write(endFlag);
		bufferedOutput.close();
	}

}
