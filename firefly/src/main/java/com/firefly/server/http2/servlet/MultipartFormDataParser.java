package com.firefly.server.http2.servlet;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Part;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class MultipartFormDataParser {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private static final int boundaryBufSize = 512;
	private static final int headBufSize = 1024;
	private static final int dataBufSize = 8 * 1024;

	public static Collection<Part> parse(final HTTP2Configuration http2Configuration, ServletInputStream input,
			String contentType, Charset encoding) throws IOException, ServletException {
		String[] contentTypeInfo = StringUtils.split(contentType, ';');
		if (contentTypeInfo == null || contentTypeInfo.length < 2)
			throw new ServletException("contentType [" + contentType + "] format error");

		String type = contentTypeInfo[0].trim().toLowerCase();
		if (!type.startsWith("multipart"))
			throw new ServletException("contentType [" + contentType + "] not multipart form data");

		String boundaryInfo = contentTypeInfo[1].trim();
		if (!boundaryInfo.startsWith("boundary="))
			throw new ServletException("boundary [" + contentType + "] format error");

		final String boundary = boundaryInfo.substring("boundary=".length());

		if (boundary.length() == 0)
			throw new ServletException("boundary [" + boundary + "] format error");

		Collection<Part> collection = new LinkedList<Part>();
		Status status = Status.BOUNDARY;
		PartImpl part = null;

		byte[] buf = new byte[boundaryBufSize];
		byte[] last = null;

		for (int len = 0; ((len = input.readLine(buf, 0, buf.length)) != -1);) {
			switch (status) {
			case BOUNDARY:
				if (buf[len - 1] != '\n')
					throw new ServletException("boundary format error");

				String currentBoundary = new String(buf, 0, len, encoding).trim();
				buf = new byte[headBufSize];

				if (!currentBoundary.equals("--" + boundary))
					throw new ServletException("boundary [" + currentBoundary + "] format error");

				part = new PartImpl(http2Configuration);
				status = Status.HEAD;
				break;

			case HEAD:
				if (buf[len - 1] != '\n')
					throw new ServletException("head format error");

				String headInfo = new String(buf, 0, len, encoding).trim();
				if (VerifyUtils.isEmpty(headInfo)) {
					buf = new byte[dataBufSize];
					status = Status.DATA;
					break;
				}

				buf = new byte[headBufSize];

				String[] t = StringUtils.split(headInfo, ":", 2);
				if (t.length != 2)
					throw new ServletException("head [" + headInfo + "] format error");

				part.headMap.put(t[0].toLowerCase(), t[1]);
				break;

			case DATA:
				byte[] data = Arrays.copyOf(buf, len);
				buf = new byte[dataBufSize];

				if (Arrays.equals(data, ("--" + boundary + "\r\n").getBytes(encoding))) {
					if (last != null) {
						// remove last '\r\n'
						log.debug("into finish last len: {}", last.length);
						part.getOutputStream().write(last, 0, last.length - 2);
						part.size += last.length - 2;
						last = null;
					}

					part.getOutputStream().close();
					collection.add(part);
					status = Status.HEAD;
					part = new PartImpl(http2Configuration);
					break;
				} else if (Arrays.equals(data, ("--" + boundary + "--\r\n").getBytes(encoding))) {
					if (last != null) {
						// remove last '\r\n'
						log.debug("into end last len: {}", last.length);
						part.getOutputStream().write(last, 0, last.length - 2);
						part.size += last.length - 2;
						last = null;
					}

					part.getOutputStream().close();
					collection.add(part);
					status = Status.FINISH;
					break;
				} else {
					if (len > 2) {
						if (last != null) {
							part.getOutputStream().write(last);
							part.size += last.length;
						}
						last = data;
					} else {
						// TODO need more test case
						if (last != null) {
							byte[] temp = new byte[last.length + data.length];
							System.arraycopy(last, 0, temp, 0, last.length);
							System.arraycopy(data, 0, temp, last.length, data.length);
							last = temp;
						} else {
							last = data;
						}
						log.debug("data line length: {}", len);
					}
					break;
				}
			default:
				break;
			}

		}

		return collection;
	}

	private enum Status {
		BOUNDARY, HEAD, DATA, FINISH
	}
}
