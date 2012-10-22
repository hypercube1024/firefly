package com.firefly.mvc.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface WebHandler {
	View invoke(HttpServletRequest request, HttpServletResponse response);
}
