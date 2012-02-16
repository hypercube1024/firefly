package com.firefly.mvc.web.support;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ViewHandle {
	void render(HttpServletRequest request, HttpServletResponse response,
			Object view) throws ServletException, IOException;
}
