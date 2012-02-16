package com.firefly.benchmark.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.Controller;
import com.firefly.annotation.RequestMapping;

@Controller
public class IndexController {
	@RequestMapping(value = "/index")
	public String index(HttpServletRequest request, HttpServletResponse response) {
		request.setAttribute("hello", "hello firefly");
		return "/index.html";
	}
}
