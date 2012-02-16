package test.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import test.mixed.Food;
import test.mixed.FoodService;
import com.firefly.annotation.Inject;
import com.firefly.annotation.Interceptor;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

@Interceptor(uri = "/food*")
public class FoodInterceptor {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	@Inject
	private FoodService foodService;

	public String before(HttpServletRequest request, HttpServletResponse response) {
		log.info("before 0 [{}]", request.getRequestURI());
		String fruit = request.getParameter("fruit");
		Food food = foodService.getFood(fruit);
		if(food != null) {
			request.setAttribute("fruit", food);
			return "/food.jsp";
		} else 
			return null;
	}

	public void after(HttpServletRequest request, HttpServletResponse response, String view, String str2) {
		log.info("after 0 [{}], return [{}], str2 [{}]", request.getRequestURI(), view, str2);
		request.setAttribute("returnView", view);
		request.setAttribute("str2", str2);
	}
}
