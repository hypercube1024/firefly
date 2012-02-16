package test.controller;

import javax.servlet.http.HttpServletRequest;

import test.mixed.Food;

import com.firefly.annotation.Controller;
import com.firefly.annotation.RequestMapping;

@Controller
public class FoodController {

	@RequestMapping(value = "/food")
	public String getFood(HttpServletRequest request) {
		Food food = new Food();
		food.setName("orange");
		food.setPrice(3.5);
		request.setAttribute("fruit", food);
		return "/food.jsp";
	}
	
	@RequestMapping(value = "/food/view1")
	public String getFoodView(HttpServletRequest request) {
		return "/foodView1.jsp";
	}
	
}
