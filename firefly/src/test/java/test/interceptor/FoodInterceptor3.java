package test.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import test.mixed.Food;

import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.JsonView;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

@Interceptor(uri = "/*/view*", order = 2)
public class FoodInterceptor3 {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public View dispose(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
		Food food = new Food();
		food.setName("banana");
		food.setPrice(3.99);
		log.info("food interceptor 2 : {}", food);
		return new JsonView(food);
	}
}
