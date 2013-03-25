package com.firefly.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import com.firefly.utils.StringUtils;

public class MultipartFormDataParser {

	public static Collection<Part> parse(InputStream input, String contentType, String charset) throws IOException, ServletException {
		String[] contentTypeInfo = StringUtils.split(contentType, ';');
		if(contentTypeInfo == null || contentTypeInfo.length < 2)
			throw new IllegalArgumentException("contentType '" + contentType + "' format error");
		
		String type = contentTypeInfo[0].trim().toLowerCase();
		if(!"multipart/form-data".equals(type))
			throw new IllegalArgumentException("contentType '" + contentType + "' not multipart form data");
		
		String boundaryInfo = contentTypeInfo[1].trim();
		if(!boundaryInfo.startsWith("boundary="))
			throw new IllegalArgumentException("boundary '" + contentType + "' format error");
		
		String boundary = boundaryInfo.substring("boundary=".length());
		if(boundary.length() == 0)
			throw new IllegalArgumentException("boundary '" + contentType + "' format error");

		if(!boundary.startsWith("----"))
			throw new IllegalArgumentException("boundary '" + contentType + "' format error");
		
		
		return null;
	}
}
