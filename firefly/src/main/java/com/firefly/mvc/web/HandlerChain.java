package com.firefly.mvc.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HandlerChain {
	View doNext(HttpServletRequest request, HttpServletResponse response, HandlerChain chain);
}
