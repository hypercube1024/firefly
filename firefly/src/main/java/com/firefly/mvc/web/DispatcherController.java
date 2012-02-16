package com.firefly.mvc.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DispatcherController {
	void dispatcher(HttpServletRequest request, HttpServletResponse response);
}
