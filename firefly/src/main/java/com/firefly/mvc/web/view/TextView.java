package com.firefly.mvc.web.view;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.View;

public class TextView implements View {

	private static String ENCODING;
	private final String text;

	public static void setEncoding(String encoding) {
		if (ENCODING == null && encoding != null)
			ENCODING = encoding;
	}

	public TextView(String text) {
		this.text = text;
	}

	@Override
	public void render(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding(ENCODING);
		response.setHeader("Content-Type", "text/plain; charset=" + ENCODING);
		PrintWriter writer = response.getWriter();
		try {
			writer.print(text);
		} finally {
			writer.close();
		}
	}

}
