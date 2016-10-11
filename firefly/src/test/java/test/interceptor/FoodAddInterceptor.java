package test.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;

import test.mixed.Food;

@Interceptor(uri = "/food/add")
public class FoodAddInterceptor {

	public View dispose(HandlerChain chain, HttpServletRequest request, HttpServletResponse response, Food food) {
		System.out.println("food: " + food);
		request.setAttribute("foodFormInterceptor", food);
		return chain.doNext(request, response, chain);
	}
}
