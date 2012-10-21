package com.firefly.mvc.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface View {
//	String FFT = "fft";
//	String JSP = "jsp";
//	String JSON = "json";
//	String XML = "xml";
//	String TEXT = "text";
//	String REDIRECT = "redirect";
	void render(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
}
