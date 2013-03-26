package com.firefly.server.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Part;

import com.firefly.server.exception.HttpServerException;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class MultipartFormDataParser {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static final int maxBufSize = 1024;
	
	public static Collection<Part> parse(ServletInputStream input, String contentType, String charset) throws IOException, ServletException {
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
		
		Collection<Part> collection = new LinkedList<Part>();
		Status status = Status.BOUNDARY;
		PartImpl part = null;
		
		byte[] buf = new byte[maxBufSize];
		byte[] last = null;
		
		for (int len = 0; ((len = input.readLine(buf, 0, buf.length)) != -1);) {
			switch (status) {
			case BOUNDARY:
				String currentBoundary = new String(buf, 0, len, charset).trim();
				buf = new byte[maxBufSize];
				
				if(!currentBoundary.equals("--" + boundary))
					throw new HttpServerException("boundary [" + currentBoundary + "] format error");
			
				part = new PartImpl();
				status = Status.HEAD;
				break;
				
			case HEAD:
				String headInfo = new String(buf, 0, len, charset).trim();
				buf = new byte[maxBufSize];
				
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
				byte[] data = Arrays.copyOf(buf, len);
				buf = new byte[maxBufSize];
				
				if(Arrays.equals(data, ("--" + boundary + "\r\n").getBytes(charset))) {
					if(last != null) {						
						// remove last '\r\n'
						log.debug("into finish last len: {}", last.length);
						part.getOutputStream().write(last, 0 ,last.length - 2);
						part.size += last.length - 2;
						last = null;
					}
					
					part.getOutputStream().close();
					collection.add(part);
					status = Status.HEAD;
					part = new PartImpl();
					break;
				} else if(Arrays.equals(data, ("--" + boundary + "--\r\n").getBytes(charset))) {
					if(last != null) {
						// remove last '\r\n'
						log.debug("into end last len: {}", last.length);
						part.getOutputStream().write(last, 0 ,last.length - 2);
						part.size += last.length - 2;
						last = null;
					}
					
					part.getOutputStream().close();
					collection.add(part);
					status = Status.FINISH;
					break;
				} else {
					if(len > 2) {
						if(last != null) {
							part.getOutputStream().write(last);
							part.size += last.length;
						}
						last = data;
					} else { 
						// TODO need more test case
						if(last != null) {
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
