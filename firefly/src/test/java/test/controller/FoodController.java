package test.controller;

import javax.servlet.http.HttpServletRequest;

import test.mixed.Food;

import com.firefly.annotation.Controller;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.JspView;

@Controller
public class FoodController {

	@RequestMapping(value = "/food")
	public View getFood(HttpServletRequest request) {
		Food food = new Food();
		food.setName("orange");
		food.setPrice(3.5);
		request.setAttribute("fruit", food);
		return new JspView("/food.jsp");
	}

	@RequestMapping(value = "/food/view1")
	public View getFoodView(HttpServletRequest request) {
		return new JspView("/foodView1.jsp");
	}

	@RequestMapping(value = "/food/add", method = { HttpMethod.POST })
	public View addFood(Food food, HttpServletRequest request) {
		System.out.println("food form -> " + food);
		request.setAttribute("foodForm", food);
		return new JspView("/food.jsp");
	}

}
