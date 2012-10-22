package test.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

@Interceptor(uri = "/*/view*", order = 2)
public class FoodInterceptor3 {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public View dispose(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
		return null;
	}
}
