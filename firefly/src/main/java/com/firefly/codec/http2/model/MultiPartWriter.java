package com.firefly.codec.http2.model;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

public class MultiPartWriter extends FilterWriter {

	private final static String __CRLF = "\015\012";
	private final static String __DASHDASH = "--";

	public static final String MULTIPART_MIXED = MultiPartOutputStream.MULTIPART_MIXED;
	public static final String MULTIPART_X_MIXED_REPLACE = MultiPartOutputStream.MULTIPART_X_MIXED_REPLACE;

	private String boundary;

	private boolean inPart = false;

	public MultiPartWriter(Writer out) throws IOException {
		super(out);
		boundary = "firefly" + System.identityHashCode(this) + Long.toString(System.currentTimeMillis(), 36);

		inPart = false;
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
			out.write(boundary);
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

	/**
	 * Start creation of the next Content.
	 * 
	 * @param contentType
	 *            the content type
	 * @throws IOException
	 *             if unable to write the part
	 */
	public void startPart(String contentType) throws IOException {
		if (inPart)
			out.write(__CRLF);
		out.write(__DASHDASH);
		out.write(boundary);
		out.write(__CRLF);
		out.write("Content-Type: ");
		out.write(contentType);
		out.write(__CRLF);
		out.write(__CRLF);
		inPart = true;
	}

	/**
	 * end creation of the next Content.
	 * 
	 * @throws IOException
	 *             if unable to write the part
	 */
	public void endPart() throws IOException {
		if (inPart)
			out.write(__CRLF);
		inPart = false;
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
		out.write(__DASHDASH);
		out.write(boundary);
		out.write(__CRLF);
		out.write("Content-Type: ");
		out.write(contentType);
		out.write(__CRLF);
		for (int i = 0; headers != null && i < headers.length; i++) {
			out.write(headers[i]);
			out.write(__CRLF);
		}
		out.write(__CRLF);
		inPart = true;
	}

}
