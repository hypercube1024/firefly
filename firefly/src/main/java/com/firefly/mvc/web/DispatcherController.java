package com.firefly.mvc.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DispatcherController {
	
	/**
	 * 派发http请求
	 * @param request HttpServletRequest对象
	 * @param response HttpServletResponse对象
	 * @return 是否继续执行下一个派发
	 */
	boolean dispatcher(HttpServletRequest request, HttpServletResponse response);
}
