package com.firefly.mvc.web.view;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.View;

public class JspView implements View {

	private static String VIEW_PATH;
	private final String page;
	
	public JspView(String page) {
		this.page = page;
	}
	
	public static void setViewPath(String path) {
		if(VIEW_PATH == null && path != null)
			VIEW_PATH = path;
	}

	public String getPage() {
		return page;
	}

	@Override
	public void render(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String ret = VIEW_PATH + page;
		request.getRequestDispatcher(ret).forward(request, response);
	}

}
