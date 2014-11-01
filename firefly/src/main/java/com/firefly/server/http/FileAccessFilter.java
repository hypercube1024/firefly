package com.firefly.server.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface FileAccessFilter {
	/**
	 * The static file access filter
	 * @param request HTTP request
	 * @param response HTTP response
	 * @param path The current file path
	 * @return Return a file name，when you return null that represents to skip the process，
	 * meanwhile you must do a HTTP response in this method。
	 */
	String doFilter(HttpServletRequest request, HttpServletResponse response, String path);
}
