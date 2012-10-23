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

@Interceptor(uri = "/food/view*", order = 1)
public class FoodInterceptor2 {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	@Inject
	private FoodService foodService;

	public View dispose(HttpServletResponse response, HttpServletRequest request, HandlerChain chain) {
		Food food = new Food();
		food.setName("ananas");
		food.setPrice(4.99);
		request.setAttribute("fruit1", food);
		log.info("food interceptor 1 : {}", food);
		return chain.doNext(request, response, chain);
	}
}
