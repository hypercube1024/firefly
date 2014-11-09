package com.fireflysource.demo.controller;

import com.firefly.annotation.Controller;
import com.firefly.annotation.Inject;
import com.firefly.annotation.PathVariable;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.JsonView;
import com.fireflysource.demo.service.FruitService;

@Controller
public class FruitController {
	
	@Inject
	private FruitService fruitService;
	
	@RequestMapping(value = "/fruit/?")
	public View getFruit(@PathVariable String[] args) {
		String title = args[0];
		return new JsonView(fruitService.getFruitByTitle(title));
	}
}
