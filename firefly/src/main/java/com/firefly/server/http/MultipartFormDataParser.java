package com.firefly.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

public class MultipartFormDataParser {

	public static Collection<Part> parse(InputStream input, String boundary, String charset) throws IOException, ServletException {
		return null;
	}
}
