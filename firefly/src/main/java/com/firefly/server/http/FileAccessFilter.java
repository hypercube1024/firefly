package com.firefly.server.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface FileAccessFilter {
	/**
	 * 静态文件访问过滤器
	 * @param request 
	 * @param response
	 * @return 需要输出的静态文件名，返回null则跳过后续处理，此时需要在函数内做出http响应。
	 */
	String doFilter(HttpServletRequest request, HttpServletResponse response);
}
