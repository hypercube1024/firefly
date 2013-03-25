package com.firefly.server.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import com.firefly.server.exception.HttpServerException;
import com.firefly.server.io.FilePipedStream;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;

public class MultipartFormDataParser {

//	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	public static Collection<Part> parse(InputStream input, String contentType, String charset, String tempdir) throws IOException, ServletException {
		String[] contentTypeInfo = StringUtils.split(contentType, ';');
		if(contentTypeInfo == null || contentTypeInfo.length < 2)
			throw new IllegalArgumentException("contentType [" + contentType + "] format error");
		
		String type = contentTypeInfo[0].trim().toLowerCase();
		if(!type.startsWith("multipart"))
			throw new IllegalArgumentException("contentType [" + contentType + "] not multipart form data");
		
		String boundaryInfo = contentTypeInfo[1].trim();
		if(!boundaryInfo.startsWith("boundary="))
			throw new IllegalArgumentException("boundary [" + contentType + "] format error");
		
		final String boundary = boundaryInfo.substring("boundary=".length());
		if(boundary.length() == 0)
			throw new IllegalArgumentException("boundary [" + boundary + "] format error");

		if(!boundary.startsWith("----"))
			throw new IllegalArgumentException("boundary [" + boundary + "] format error");
		
		Collection<Part> collection = new LinkedList<Part>();
		Status status = Status.BOUNDARY;
		ByteArrayOutputStream byteBuf = new ByteArrayOutputStream();
		PartImpl part = null;
		
		for (int b = 0; ((b = input.read()) != -1); ) {
			byteBuf.write(b);
			if(b == '\n') {
				switch (status) {
				case BOUNDARY:
					String currentBoundary = new String(byteBuf.toByteArray(), charset).trim();
					byteBuf = new ByteArrayOutputStream();
					
					if(!currentBoundary.equals("--" + boundary))
						throw new HttpServerException("boundary [" + currentBoundary + "] format error");
					
					// TODO need use mixedPipedStream
					part = new PartImpl(new FilePipedStream(tempdir, "part-"+UUID.randomUUID().toString()));
					status = Status.HEAD;
					break;
					
				case HEAD:
					String headInfo = new String(byteBuf.toByteArray(), charset).trim();
					byteBuf = new ByteArrayOutputStream(512);
					
					if(VerifyUtils.isEmpty(headInfo)) {
						status = Status.DATA;
						break;
					}
					
					String[] t = StringUtils.split(headInfo, ":", 2);
					if(t.length != 2)
						throw new HttpServerException("head [" + headInfo + "] format error");

					part.headMap.put(t[0].toLowerCase(), t[1]);
					break;
					
				case DATA:
					byte[] data = byteBuf.toByteArray();
					byteBuf = new ByteArrayOutputStream(1024);
					
					if(Arrays.equals(data, ("--" + boundary + "\r\n").getBytes(charset))) {
						part.getOutputStream().close();
						collection.add(part);
						status = Status.HEAD;
						// TODO need use mixedPipedStream
						part = new PartImpl(new FilePipedStream(tempdir, "part-"+UUID.randomUUID().toString()));
						break;
					} else if(Arrays.equals(data, ("--" + boundary + "--\r\n").getBytes(charset))) {
						part.getOutputStream().close();
						collection.add(part);
						status = Status.FINISH;
						break;
					} else {
						part.getOutputStream().write(data);
						part.size += data.length;
						break;
					}
				default:
					break;
				}
			}
		}
		
		return collection;
	}
	
	private enum Status {
		BOUNDARY, HEAD, DATA, FINISH
	}
}
