package com.firefly.mvc.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DispatcherController {
	
	/**
	 * 前端控制器，派发http请求
	 * @param request HttpServletRequest对象
	 * @param response HttpServletResponse对象
	 */
	void dispatcher(HttpServletRequest request, HttpServletResponse response);
}
