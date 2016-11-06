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

@Interceptor(uri = "/food/view*", order = 1)
public class FoodInterceptor2 {
	private static Logger log = LoggerFactory.getLogger("firefly-system");
	@Inject
	private FoodService foodService;

	public View dispose(HttpServletResponse response, HttpServletRequest request, HandlerChain chain) {
		Food food = new Food();
		food.setName("ananas");
		food.setPrice(4.99);
		request.setAttribute("fruit1", food);
		log.info("start food interceptor 1");
		View view = chain.doNext(request, response, chain);
		log.info("end food interceptor 1 : {}", food);
		return view;
	}
}
