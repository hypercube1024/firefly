package test.interceptor;

import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.JsonView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.mixed.Food;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Interceptor(uri = "/*/view*", order = 2)
public class FoodInterceptor3 {
	private static Logger log = LoggerFactory.getLogger("firefly-system");

	public View dispose(HttpServletRequest request, HttpServletResponse response) {
		Food food = new Food();
		food.setName("banana");
		food.setPrice(3.99);
		log.info("food interceptor 2 : {}", food);
		return new JsonView<>(food);
	}
}
