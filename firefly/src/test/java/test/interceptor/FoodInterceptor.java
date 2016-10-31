package test.interceptor;

import com.firefly.annotation.Inject;
import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.mixed.Food;
import test.mixed.FoodService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Interceptor(uri = "/food*")
public class FoodInterceptor {
	private static Logger log = LoggerFactory.getLogger("firefly-system");
	
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
