package com.firefly.server.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface FileAccessFilter {
	String doFilter(HttpServletRequest request, HttpServletResponse response);
}
