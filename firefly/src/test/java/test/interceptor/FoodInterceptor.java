package test.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	
	public View dispose(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
		return null;
	}
}
