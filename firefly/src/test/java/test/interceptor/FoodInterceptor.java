package test.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import test.mixed.Food;
import test.mixed.FoodService;

import com.firefly.annotation.Inject;
import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

@Interceptor(uri = "/food*")
public class FoodInterceptor {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	@Inject
	private FoodService foodService;
	
	public View dispose(HandlerChain chain, HttpServletRequest request, HttpServletResponse response) {
		Food food = new Food();
		food.setName("apple");
		food.setPrice(8.0);
		request.setAttribute("fruit0", food);
		
		food = foodService.getFood("strawberry");
		request.setAttribute("strawberry", food);
		log.info("food interceptor 0 : {}", food);
		
		return chain.doNext(request, response, chain);
	}
}
