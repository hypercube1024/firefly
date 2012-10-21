package com.firefly.server.http;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.view.TemplateView;

public class RequestDispatcherImpl implements RequestDispatcher {

	private boolean forward = false;
	String path;

	@Override
	public void forward(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		if (!forward) {
			new TemplateView(path).render((HttpServletRequest) request, (HttpServletResponse) response);
			forward = true;
		}

	}

	@Override
	public void include(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		new TemplateView(path).render((HttpServletRequest) request, (HttpServletResponse) response);
	}

}
