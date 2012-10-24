package com.firefly.benchmark.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.Controller;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.TemplateView;

@Controller
public class IndexController {
	@RequestMapping(value = "/index")
	public View index(HttpServletRequest request, HttpServletResponse response) {
		request.setAttribute("hello", "hello firefly");
		return new TemplateView("/index.html");
	}
}
