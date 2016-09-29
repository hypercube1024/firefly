package com.firefly.codec.http2.model;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MultiPartOutputStream extends FilterOutputStream {

	private static final byte[] __CRLF = { '\r', '\n' };
	private static final byte[] __DASHDASH = { '-', '-' };

	public static final String MULTIPART_MIXED = "multipart/mixed";
	public static final String MULTIPART_X_MIXED_REPLACE = "multipart/x-mixed-replace";

	private final String boundary;
	private final byte[] boundaryBytes;

	private boolean inPart = false;

	public MultiPartOutputStream(OutputStream out) throws IOException {
		super(out);

		boundary = "firefly" + System.identityHashCode(this) + Long.toString(System.currentTimeMillis(), 36);
		boundaryBytes = boundary.getBytes(StandardCharsets.ISO_8859_1);
	}

	public MultiPartOutputStream(OutputStream out, String boundary) throws IOException {
		super(out);

		this.boundary = boundary;
		boundaryBytes = boundary.getBytes(StandardCharsets.ISO_8859_1);
	}

	/**
	 * End the current part.
	 * 
	 * @exception IOException
	 *                IOException
	 */
	@Override
	public void close() throws IOException {
		try {
			if (inPart)
				out.write(__CRLF);
			out.write(__DASHDASH);
			out.write(boundaryBytes);
			out.write(__DASHDASH);
			out.write(__CRLF);
			inPart = false;
		} finally {
			super.close();
		}
	}

	public String getBoundary() {
		return boundary;
	}

	public OutputStream getOut() {
		return out;
	}

	/**
	 * Start creation of the next Content.
	 * 
	 * @param contentType
	 *            the content type of the part
	 * @throws IOException
	 *             if unable to write the part
	 */
	public void startPart(String contentType) throws IOException {
		if (inPart)
			out.write(__CRLF);
		inPart = true;
		out.write(__DASHDASH);
		out.write(boundaryBytes);
		out.write(__CRLF);
		if (contentType != null)
			out.write(("Content-Type: " + contentType).getBytes(StandardCharsets.ISO_8859_1));
		out.write(__CRLF);
		out.write(__CRLF);
	}

	/**
	 * Start creation of the next Content.
	 * 
	 * @param contentType
	 *            the content type of the part
	 * @param headers
	 *            the part headers
	 * @throws IOException
	 *             if unable to write the part
	 */
	public void startPart(String contentType, String[] headers) throws IOException {
		if (inPart)
			out.write(__CRLF);
		inPart = true;
		out.write(__DASHDASH);
		out.write(boundaryBytes);
		out.write(__CRLF);
		if (contentType != null)
			out.write(("Content-Type: " + contentType).getBytes(StandardCharsets.ISO_8859_1));
		out.write(__CRLF);
		for (int i = 0; headers != null && i < headers.length; i++) {
			out.write(headers[i].getBytes(StandardCharsets.ISO_8859_1));
			out.write(__CRLF);
		}
		out.write(__CRLF);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}
}
