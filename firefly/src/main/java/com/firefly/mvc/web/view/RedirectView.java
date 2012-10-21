package com.firefly.mvc.web.view;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.View;

public class RedirectView implements View {
	
	private String uri;

	public RedirectView(String uri) {
		this.uri = uri;
	}

	@Override
	public void render(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendRedirect(request.getContextPath() + request.getServletPath() + uri);
	}

}
