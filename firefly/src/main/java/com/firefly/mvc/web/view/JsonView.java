package com.firefly.mvc.web.view;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.View;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.utils.json.Json;

public class JsonView implements View {
	
	private static String ENCODING;
	private final Object obj;
	
	public static void setEncoding(String encoding) {
		if(ENCODING == null && encoding != null)
			ENCODING = encoding;
	}
	
	public JsonView(Object obj) {
		this.obj = obj;
	}

	public Object getObj() {
		return obj;
	}

	@Override
	public void render(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if(obj == null) {
			SystemHtmlPage.responseSystemPage(request, response,
					ENCODING, HttpServletResponse.SC_NOT_FOUND,
					request.getRequestURI() + " not found");
			return;
		}
		response.setCharacterEncoding(ENCODING);
		response.setHeader("Content-Type", "application/json; charset=" + ENCODING);
		PrintWriter writer = response.getWriter();
		try{
			writer.print(Json.toJson(obj));
		} finally {
			writer.close();
		}
	}

}
